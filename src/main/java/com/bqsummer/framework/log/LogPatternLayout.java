package com.bqsummer.framework.log;

import ch.qos.logback.classic.pattern.*;
import ch.qos.logback.classic.pattern.color.HighlightingCompositeConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.ConverterUtil;
import ch.qos.logback.core.pattern.DynamicConverter;
import ch.qos.logback.core.pattern.color.*;
import ch.qos.logback.core.pattern.parser.Node;
import ch.qos.logback.core.pattern.parser.Parser;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.StatusManager;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class LogPatternLayout extends LayoutBase<ILoggingEvent> {
    public static final Map<String, Supplier<DynamicConverter>> defaultConverterMap = new HashMap();
    Converter<ILoggingEvent> head;
    String pattern;
    Map<String, Supplier<DynamicConverter>> instanceConverterMap = new HashMap();
    protected boolean outputPatternAsHeader = false;

    public LogPatternLayout() {
    }

    public Map<String, Supplier<DynamicConverter>> getDefaultConverterMap() {
        return defaultConverterMap;
    }

    public Map<String, Supplier<DynamicConverter>> getEffectiveConverterMap() {
        Map<String, Supplier<DynamicConverter>> effectiveMap = new HashMap<>();
        Map<String, Supplier<DynamicConverter>> defaultMap = this.getDefaultConverterMap();
        if (defaultMap != null) {
            effectiveMap.putAll(defaultMap);
        }

        Context context = this.getContext();
        if (context != null) {
            Map<String, Supplier<DynamicConverter>> contextMap = (Map) context.getObject("PATTERN_RULE_REGISTRY");
            if (contextMap != null) {
                effectiveMap.putAll(contextMap);
            }
        }

        effectiveMap.putAll(this.instanceConverterMap);
        return effectiveMap;
    }

    public void start() {
        if (this.pattern != null && this.pattern.length() != 0) {
            try {
                Parser<ILoggingEvent> p = new Parser(this.pattern);
                if (this.getContext() != null) {
                    p.setContext(this.getContext());
                }

                Node t = p.parse();
                this.head = p.compile(t, this.getEffectiveConverterMap());
                ConverterUtil.setContextForConverters(this.getContext(), this.head);
                ConverterUtil.startConverters(this.head);
                super.start();
            } catch (ScanException var3) {
                StatusManager sm = this.getContext().getStatusManager();
                sm.add(new ErrorStatus("Failed to parse pattern \"" + this.getPattern() + "\".", this, var3));
            }

        } else {
            this.addError("Empty or null pattern.");
        }
    }

    /**
     * @deprecated
     */
    protected void setContextForConverters(Converter<ILoggingEvent> head) {
        ConverterUtil.setContextForConverters(this.getContext(), head);
    }

    protected String writeLoopOnConverters(ILoggingEvent event) {
        StringBuilder strBuilder = new StringBuilder(256);

        for (Converter c = this.head; c != null; c = c.getNext()) {
            if (!StringUtils.isEmpty(MDC.get("logType")) && c instanceof MessageConverter) {
                this.appendTraceInfo(c, strBuilder, event);
            } else if (c instanceof MessageConverter) {
                c.write(strBuilder, event);
                strBuilder.append(" [ ");
                String traceId = MDC.get("traceId");
                if (!StringUtils.isEmpty(traceId)) {
                    strBuilder.append(traceId);
                } else {
                    strBuilder.append("noTraceId");
                }

                strBuilder.append(" ]");
            } else {
                c.write(strBuilder, event);
            }
        }

        return strBuilder.toString();
    }

    private void appendTraceInfo(Converter<ILoggingEvent> c, StringBuilder strBuilder, ILoggingEvent event) {
        String logMessage = c.convert(event);

        try {
            strBuilder.append(logMessage.substring(0, logMessage.lastIndexOf("}")));
            strBuilder.append(",\"traceId\":\"").append(StringUtils.isEmpty(MDC.get("traceId")) ? "noTraceId" : MDC.get("traceId")).append("\"");
            strBuilder.append("}");
        } catch (Exception var8) {
            strBuilder.append(logMessage);
        }
    }

    public String getPattern() {
        return this.pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String toString() {
        return this.getClass().getName() + "(\"" + this.getPattern() + "\")";
    }

    public Map<String, Supplier<DynamicConverter>> getInstanceConverterMap() {
        return this.instanceConverterMap;
    }

    protected String getPresentationHeaderPrefix() {
        return "";
    }

    public boolean isOutputPatternAsHeader() {
        return this.outputPatternAsHeader;
    }

    public void setOutputPatternAsHeader(boolean outputPatternAsHeader) {
        this.outputPatternAsHeader = outputPatternAsHeader;
    }

    public String doLayout(ILoggingEvent event) {
        return !this.isStarted() ? "" : this.writeLoopOnConverters(event);
    }

    public String getPresentationHeader() {
        return this.outputPatternAsHeader ? this.getPresentationHeaderPrefix() + this.pattern : super.getPresentationHeader();
    }

    static {
        defaultConverterMap.putAll(Parser.DEFAULT_COMPOSITE_CONVERTER_MAP);
        defaultConverterMap.put("d", DateConverter::new);
        defaultConverterMap.put("date", DateConverter::new);
        defaultConverterMap.put("r", RelativeTimeConverter::new);
        defaultConverterMap.put("relative", RelativeTimeConverter::new);
        defaultConverterMap.put("level", LevelConverter::new);
        defaultConverterMap.put("le", LevelConverter::new);
        defaultConverterMap.put("p", LevelConverter::new);
        defaultConverterMap.put("t", ThreadConverter::new);
        defaultConverterMap.put("thread", ThreadConverter::new);
        defaultConverterMap.put("lo", LoggerConverter::new);
        defaultConverterMap.put("logger", LoggerConverter::new);
        defaultConverterMap.put("c", LoggerConverter::new);
        defaultConverterMap.put("m", MessageConverter::new);
        defaultConverterMap.put("msg", MessageConverter::new);
        defaultConverterMap.put("message", MessageConverter::new);
        defaultConverterMap.put("C", ClassOfCallerConverter::new);
        defaultConverterMap.put("class", ClassOfCallerConverter::new);
        defaultConverterMap.put("M", MethodOfCallerConverter::new);
        defaultConverterMap.put("method", MethodOfCallerConverter::new);
        defaultConverterMap.put("L", LineOfCallerConverter::new);
        defaultConverterMap.put("line", LineOfCallerConverter::new);
        defaultConverterMap.put("F", FileOfCallerConverter::new);
        defaultConverterMap.put("file", FileOfCallerConverter::new);
        defaultConverterMap.put("X", MDCConverter::new);
        defaultConverterMap.put("mdc", MDCConverter::new);
        defaultConverterMap.put("ex", ThrowableProxyConverter::new);
        defaultConverterMap.put("exception", ThrowableProxyConverter::new);
        defaultConverterMap.put("rEx", RootCauseFirstThrowableProxyConverter::new);
        defaultConverterMap.put("rootException", RootCauseFirstThrowableProxyConverter::new);
        defaultConverterMap.put("throwable", ThrowableProxyConverter::new);
        defaultConverterMap.put("xEx", ExtendedThrowableProxyConverter::new);
        defaultConverterMap.put("xException", ExtendedThrowableProxyConverter::new);
        defaultConverterMap.put("xThrowable", ExtendedThrowableProxyConverter::new);
        defaultConverterMap.put("nopex", NopThrowableInformationConverter::new);
        defaultConverterMap.put("nopexception", NopThrowableInformationConverter::new);
        defaultConverterMap.put("cn", ContextNameConverter::new);
        defaultConverterMap.put("contextName", ContextNameConverter::new);
        defaultConverterMap.put("caller", CallerDataConverter::new);
        defaultConverterMap.put("marker", MarkerConverter::new);
        defaultConverterMap.put("property", PropertyConverter::new);
        defaultConverterMap.put("n", LineSeparatorConverter::new);
        defaultConverterMap.put("black", BlackCompositeConverter::new);
        defaultConverterMap.put("red", RedCompositeConverter::new);
        defaultConverterMap.put("green", GreenCompositeConverter::new);
        defaultConverterMap.put("yellow", YellowCompositeConverter::new);
        defaultConverterMap.put("blue", BlueCompositeConverter::new);
        defaultConverterMap.put("magenta", MagentaCompositeConverter::new);
        defaultConverterMap.put("cyan", CyanCompositeConverter::new);
        defaultConverterMap.put("white", WhiteCompositeConverter::new);
        defaultConverterMap.put("gray", GrayCompositeConverter::new);
        defaultConverterMap.put("boldRed", BoldRedCompositeConverter::new);
        defaultConverterMap.put("boldGreen", BoldGreenCompositeConverter::new);
        defaultConverterMap.put("boldYellow", BoldYellowCompositeConverter::new);
        defaultConverterMap.put("boldBlue", BoldBlueCompositeConverter::new);
        defaultConverterMap.put("boldMagenta", BoldMagentaCompositeConverter::new);
        defaultConverterMap.put("boldCyan", BoldCyanCompositeConverter::new);
        defaultConverterMap.put("boldWhite", BoldWhiteCompositeConverter::new);
        defaultConverterMap.put("highlight", HighlightingCompositeConverter::new);
        defaultConverterMap.put("lsn", LocalSequenceNumberConverter::new);
    }
}
