package org.trellisldp.camel.kafka.elasticsearch;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import java.util.List;

public class ProcessorUtils {
    private static final Logger LOGGER  = getLogger(ProcessorUtils.class);
    /**
     * Tokenize a property placeholder value
     *
     * @param context the camel context
     * @param property the name of the property placeholder
     * @param token the token used for splitting the value
     * @return a list of values
     */
    public static List<String> tokenizePropertyPlaceholder(final CamelContext context, final String property,
                                                           final String token) {
        try {
            return stream(context.resolvePropertyPlaceholders(property).split(token)).map(String::trim)
                    .filter(val -> !val.isEmpty()).collect(toList());
        } catch (final Exception ex) {
            LOGGER.debug("No property value found for {}", property);
            return emptyList();
        }
    }
}
