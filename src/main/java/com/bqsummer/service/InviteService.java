package com.bqsummer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bqsummer.framework.exception.SnorlaxClientException;
import com.bqsummer.mapper.UserMapper;
import com.bqsummer.common.dto.invite.InviteCode;
import com.bqsummer.common.dto.invite.InviteUsage;
import com.bqsummer.mapper.InviteCodeMapper;
import com.bqsummer.mapper.InviteUsageMapper;
import com.bqsummer.common.vo.req.invite.CreateInviteCodeRequest;
import com.bqsummer.common.vo.req.invite.RedeemInviteRequest;
import com.bqsummer.util.InviteCodeUtil;
import com.bqsummer.service.notify.EmailTemplate;
import com.bqsummer.service.notify.MessageCenterService;
import com.bqsummer.service.notify.NotifyUser;
import com.bqsummer.common.vo.req.point.EarnPointsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteCodeMapper codeMapper;
    private final InviteUsageMapper usageMapper;
    private final PointsService pointsService;
    private final UserMapper userMapper;
    private final MessageCenterService messageCenterService;

    // 防刷：同一IP在rolling窗口内最大兑换次数
    private static final int MAX_REDEEM_PER_IP_PER_DAY = 20;
    private static final int DEFAULT_CODE_LENGTH = 10;

    // 积分奖励配置
    private static final long REWARD_INVITER = 100L;
    private static final long REWARD_INVITEE = 50L;
    private static final String ACT_INVITE = "INVITE_REWARD";
    private static final String ACT_INVITED = "INVITED_BONUS";

    @Transactional
    public InviteCode createCode(Long creatorUserId, CreateInviteCodeRequest req) {
        if (req.getMaxUses() == null || req.getMaxUses() <= 0) {
            throw new SnorlaxClientException(400, "maxUses must be positive");
        }
        LocalDateTime expireAt = resolveExpireAt(req.getExpireAt(), req.getExpireDays());
        String code;
        InviteCode exist;
        int tryCount = 0;
        do {
            code = InviteCodeUtil.generateCode(DEFAULT_CODE_LENGTH);
            exist = codeMapper.findByCode(code);
            tryCount++;
            if (tryCount > 5) {
                // 极小概率冲突，继续增加长度再尝试
                code = InviteCodeUtil.generateCode(DEFAULT_CODE_LENGTH + 2);
                exist = codeMapper.findByCode(code);
                break;
            }
        } while (exist != null);

        InviteCode ic = new InviteCode();
        ic.setCode(code);
        ic.setCodeHash(InviteCodeUtil.sha256Hex(code));
        ic.setCreatorUserId(creatorUserId);
        ic.setMaxUses(req.getMaxUses());
        ic.setUsedCount(0);
        ic.setStatus("ACTIVE");
        ic.setExpireAt(expireAt);
        ic.setRemark(req.getRemark());
        codeMapper.insert(ic);
        return ic;
    }

    public ValidateResult validateCode(String code) {
        InviteCode ic = codeMapper.findByCode(code);
        if (ic == null) return ValidateResult.invalid("NOT_FOUND");
        if (!Objects.equals(ic.getStatus(), "ACTIVE")) return ValidateResult.invalid(ic.getStatus());
        if (ic.getExpireAt() != null && ic.getExpireAt().isBefore(LocalDateTime.now())) return ValidateResult.invalid("EXPIRED");
        if (ic.getUsedCount() >= ic.getMaxUses()) return ValidateResult.invalid("USED");
        int remaining = ic.getMaxUses() - ic.getUsedCount();
        return ValidateResult.valid(remaining, ic.getExpireAt());
    }

    @Transactional
    public void revoke(Long requesterId, Long codeId) {
        InviteCode found = codeMapper.selectById(codeId);
        if (found == null) throw new SnorlaxClientException(404, "invite code not found");
        if (found.getCreatorUserId() != null && !found.getCreatorUserId().equals(requesterId)) {
            throw new SnorlaxClientException(403, "no permission to revoke");
        }
        codeMapper.updateStatus(codeId, "REVOKED");
    }

    @Transactional
    public RedeemResult redeem(Long inviteeUserId, RedeemInviteRequest req, String clientIp, String userAgent) {
        if (!StringUtils.hasText(req.getCode())) throw new SnorlaxClientException(400, "code required");

        // 每个用户仅可使用一次
        if (usageMapper.countByInvitee(inviteeUserId) > 0) {
            throw new SnorlaxClientException(400, "user already redeemed");
        }
        // IP防刷：每天最多次数
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        if (StringUtils.hasText(clientIp) && usageMapper.countByIpSince(clientIp, since) >= MAX_REDEEM_PER_IP_PER_DAY) {
            throw new SnorlaxClientException(433, "too many redeems from this IP");
        }
        InviteCode ic = codeMapper.findByCode(req.getCode());
        if (ic == null) throw new SnorlaxClientException(404, "invalid code");
        if (!"ACTIVE".equals(ic.getStatus())) throw new SnorlaxClientException(400, "code not active");
        if (ic.getExpireAt() != null && ic.getExpireAt().isBefore(LocalDateTime.now())) throw new SnorlaxClientException(400, "code expired");
        if (ic.getUsedCount() >= ic.getMaxUses()) throw new SnorlaxClientException(400, "code exhausted");
        if (ic.getCreatorUserId() != null && ic.getCreatorUserId().equals(inviteeUserId)) {
            throw new SnorlaxClientException(400, "cannot redeem self code");
        }
        int ok = codeMapper.tryConsumeOnce(ic.getId());
        if (ok == 0) throw new SnorlaxClientException(409, "code cannot be consumed now");

        // 记录使用
        InviteUsage usage = new InviteUsage();
        usage.setInviteCodeId(ic.getId());
        usage.setCodeHash(ic.getCodeHash());
        usage.setInviterUserId(ic.getCreatorUserId());
        usage.setInviteeUserId(inviteeUserId);
        usage.setClientIp(clientIp);
        usage.setUserAgent(userAgent);
        usage.setUsedAt(LocalDateTime.now());
        usageMapper.insert(usage);

        // 发放积分
        if (ic.getCreatorUserId() != null && REWARD_INVITER > 0) {
            EarnPointsRequest pr = new EarnPointsRequest();
            pr.setUserId(ic.getCreatorUserId());
            pr.setAmount(REWARD_INVITER);
            pr.setActivityCode(ACT_INVITE);
            pr.setDescription("invitee redeemed by user " + inviteeUserId);
            pointsService.earn(pr);
        }
        if (REWARD_INVITEE > 0) {
            EarnPointsRequest pr2 = new EarnPointsRequest();
            pr2.setUserId(inviteeUserId);
            pr2.setAmount(REWARD_INVITEE);
            pr2.setActivityCode(ACT_INVITED);
            pr2.setDescription("redeem invite code");
            pointsService.earn(pr2);
        }

        // 通知邀请人（如有邮箱）
        if (ic.getCreatorUserId() != null) {
            var inviter = userMapper.findById(ic.getCreatorUserId());
            if (inviter != null && StringUtils.hasText(inviter.getEmail())) {
                NotifyUser nu = NotifyUser.builder().emailAddress(inviter.getEmail()).build();
                EmailTemplate tpl = EmailTemplate.builder()
                        .title("Your invite code was used")
                        .emailBody("Your code " + ic.getCode() + " was used by user #" + inviteeUserId + ".")
                        .build();
                messageCenterService.sendTemplate(List.of(nu), List.of(tpl));
            }
        }

        int remaining = Math.max(0, ic.getMaxUses() - (ic.getUsedCount() + 1));
        return new RedeemResult(true, remaining);
    }

    public List<InviteCode> listMyCodes(Long userId) {
        return codeMapper.selectList(new LambdaQueryWrapper<InviteCode>()
                .eq(InviteCode::getCreatorUserId, userId)
                .orderByDesc(InviteCode::getCreatedTime));
    }

    private LocalDateTime resolveExpireAt(LocalDateTime explicit, Integer days) {
        if (explicit != null) return explicit;
        int d = (days != null && days > 0) ? days : 30;
        return LocalDateTime.now().plusDays(d);
    }

    // ====== DTOs for service results ======
    public record ValidateResult(boolean valid, String status, Integer remainingUses, LocalDateTime expireAt) {
        public static ValidateResult invalid(String status) {
            return new ValidateResult(false, status, 0, null);
        }
        public static ValidateResult valid(int remaining, LocalDateTime expireAt) {
            return new ValidateResult(true, "ACTIVE", remaining, expireAt);
        }
    }

    public record RedeemResult(boolean success, int remainingUses) {}
}


