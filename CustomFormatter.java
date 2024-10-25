import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//custom formatter class to format log messages
public class CustomFormatter extends Formatter {
    
    private static final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        // Adding a separator line for readability
        sb.append("\n--------------------------------------------------\n");
        String timestamp = LocalDateTime.now().format(dtFormatter);
        // Formatting the log message
        sb.append(String.format("%s: [%s] - %s\n", 
                             timestamp,
                             record.getLevel().getName(),
                             record.getMessage()));
        // Adding a separator line for readability
        sb.append("--------------------------------------------------\n");
        return sb.toString();

    }
}