package com.bqsummer.common.dto.point;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("points_deduction_detail")
public class PointsDeductionDetail {
    private Long id;
    private Long txId;     // consume transaction id
    private Long bucketId; // from which bucket
    private Long amount;   // deducted from this bucket
}

