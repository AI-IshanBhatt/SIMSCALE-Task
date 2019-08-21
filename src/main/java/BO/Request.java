package BO;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static BO.Utils.*;


public class Request {

    private final Timestamp startTime;
    private final Timestamp endTime;
    private final String traceId;
    private final String serviceName;
    private final String prevSpan;
    private final String newSpan;


    // The one who creates this object, will give it as timestamp
    public Request(Timestamp startTime, Timestamp endTime, String traceId, String serviceName, String prevSpan, String newSpan) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.traceId = traceId;
        this.serviceName = serviceName;
        this.prevSpan = prevSpan;
        this.newSpan = newSpan;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getPrevSpan() { return prevSpan; }

    public String getNewSpan() { return newSpan; }

    @Override
    public String toString() {
        return "Request{" +
                ", serviceName='" + serviceName + '\'' +
                ", newSpan='" + newSpan + '\'' +
                '}';
    }

    // Create Optional<Request> object from logLine
    public static Optional<Request> parseLine(String logLine) {
        String dateRegex = DATE_UTC_REGEX;
        String traceServiceRegex = TRACE_SERVICE_REGEX;

        String logRegex = dateRegex+" "+dateRegex+" "+traceServiceRegex+" "+traceServiceRegex+" "+SPAN_REGEX;
        Pattern p = Pattern.compile(logRegex);
        Matcher m = p.matcher(logLine);

        Request r = null;

        if (m.find()) {
            Timestamp startTime = Utils.stringToDateUTC(m.group(1));
            Timestamp endTime = Utils.stringToDateUTC(m.group(2));
            String traceId = m.group(3);
            String serviceName = m.group(4);
            String prevSpan = m.group(5);
            String newSpan = m.group(6);
            r = new Request(startTime, endTime, traceId, serviceName, prevSpan, newSpan);
        }
        else {
            System.out.println("SOMETHING WRONG IN MATCHING OF LINE "+logLine+ " IGNORING");
        }

        return Optional.ofNullable(r);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return Objects.equals(getTraceId(), request.getTraceId()) &&
                Objects.equals(getServiceName(), request.getServiceName()) &&
                Objects.equals(getPrevSpan(), request.getPrevSpan());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTraceId(), getServiceName(), getPrevSpan());
    }
}
