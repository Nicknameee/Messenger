package spring.application.tree.data.utility.converting;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

public class DateConvertingUtility {
    private final DateFormat UTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final DateFormat SERVER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final DateFormat CUSTOM = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public ZonedDateTime convertDate(TimeZone to, Date convert) throws ParseException {
        return ZonedDateTime.ofInstant(UTC.parse(SERVER.format(convert)).toInstant(), to.toZoneId());
    }

    public ZonedDateTime convertDate(TimeZone from, TimeZone to, Date convert) throws ParseException {
        CUSTOM.setTimeZone(from);
        return ZonedDateTime.ofInstant(CUSTOM.parse(SERVER.format(convert)).toInstant(), to.toZoneId());
    }

    public String format(ZonedDateTime zonedDateTime, String pattern) {
        return pattern
                .replaceAll("yyyy", String.valueOf(zonedDateTime.getYear()))
                .replaceAll("MM", String.format("%s", zonedDateTime.getMonthValue() < 10 ? "0" + zonedDateTime.getMonthValue() : zonedDateTime.getMonthValue()))
                .replaceAll("dd", String.format("%s", zonedDateTime.getDayOfMonth() < 10 ? "0" + zonedDateTime.getDayOfMonth() : zonedDateTime.getDayOfMonth()))
                .replaceAll("HH", String.format("%s", zonedDateTime.getHour() < 10 ? "0" + zonedDateTime.getHour() : zonedDateTime.getHour()))
                .replaceAll("mm", String.format("%s", zonedDateTime.getMinute() < 10 ? "0" + zonedDateTime.getMinute() : zonedDateTime.getMinute()))
                .replaceAll("ss", String.format("%s", zonedDateTime.getSecond() < 10 ? "0" + zonedDateTime.getSecond() : zonedDateTime.getSecond()))
                .replaceAll("XXX", String.valueOf(zonedDateTime.getOffset()))
                .replaceAll("ZZZ", zonedDateTime.getOffset().getId());
    }
}
