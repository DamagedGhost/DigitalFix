package com.digitalfix.ms_digitalfix_notify.admin.kafka;

import com.digitalfix.ms_digitalfix_notify.kafka.KafkaProducerProperties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class KafkaAdminClientFactory {

    private final KafkaProducerProperties props;

    public KafkaAdminClientFactory(KafkaProducerProperties props) {
        this.props = props;
    }

    public AdminClient crear() {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 8000);
        return AdminClient.create(config);
    }
}