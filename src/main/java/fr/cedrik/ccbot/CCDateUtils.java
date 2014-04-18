/**
 *
 */
package fr.cedrik.ccbot;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author C&eacute;drik LIME
 */
public class CCDateUtils {

	private CCDateUtils() {
		assert false;
	}

	public static Date parseServerDate(String dateStr) {
		Date date = null;
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//$NON-NLS-1$
			// server is elb-ccios-prod-747311989.us-east-1.elb.amazonaws.com, so should be EST ("EDT" when daylight saving time), but it looks like this is PST
			dateFormat.setTimeZone(TimeZone.getTimeZone("PST"));//FIXME "PDT" when daylight saving time?
			date = dateFormat.parse(dateStr);
		} catch (ParseException ignore) {
		}
		return date;
	}

}
