package spring.application.tree.data.utility.converting;

import java.util.Date;
import java.util.TimeZone;

public class DateConvertingUtility {
    public static Date convertDate(TimeZone from, TimeZone to, Date convert) {
        return new Date(convert.getTime() - from.getRawOffset() + to.getRawOffset());
    }

    public static Date convertDate(TimeZone to, Date convert) {
        return convertDate(TimeZone.getDefault(), to, convert);
    }
}
