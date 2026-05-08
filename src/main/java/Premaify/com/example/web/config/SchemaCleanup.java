package Premaify.com.example.web.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SchemaCleanup {
    @Bean
    CommandLineRunner removeDeprecatedPaymentColumns(JdbcTemplate jdbcTemplate) {
        return args -> {
            dropColumnIfExists(jdbcTemplate, "leads", "payment_type");
            dropColumnIfExists(jdbcTemplate, "order_logs", "payment_type");
        };
    }

    private void dropColumnIfExists(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP COLUMN " + columnName);
        } catch (Exception ignored) {
            // Column already removed or table is not ready in this environment.
        }
    }
}
