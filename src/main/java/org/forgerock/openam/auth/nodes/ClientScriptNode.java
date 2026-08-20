/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017 ForgeRock AS.
 */
/**
 * jon.knight@forgerock.com
 *
 * An authentication node which executes a Javascript on the client browser.
 */


package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.persistence.config.consumer.ScriptContext;
import javax.security.auth.callback.Callback;
import java.util.Optional;
import static org.forgerock.openam.auth.node.api.Action.send;
import javax.inject.Inject;



/**
 * A node that executes a client-side Javascript and stores any resulting output in the shared state.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
    configClass = ClientScriptNode.Config.class)
public class ClientScriptNode extends SingleOutcomeNode {

    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/ClientScriptNode";

    /**
     * The script type whose scripts are offered in the {@code script} dropdown. Matches the
     * "Client-side Authentication" script context, so only JavaScript scripts saved under that
     * type in Realms &gt; Scripts are selectable.
     */
    private static final String CLIENT_SIDE_SCRIPT_CONTEXT = "AUTHENTICATION_CLIENT_SIDE";

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The client-side script to execute, selected from the scripts registered against the
         * Client-side Authentication script context.
         * @return the script.
         */
        @Attribute(order = 100)
        @ScriptContext(CLIENT_SIDE_SCRIPT_CONTEXT)
        default Script script() {
            return Script.EMPTY_SCRIPT;
        }

        @Attribute(order = 200)
        String scriptResult();
    }

    private final Config config;

    /**
     * Guice constructor.
     * @param config The node configuration.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public ClientScriptNode(@Assisted Config config) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        Optional<String> result = context.getCallback(HiddenValueCallback.class).map(HiddenValueCallback::getValue).filter(scriptOutput -> !Strings.isNullOrEmpty(scriptOutput));
        if (result.isPresent()) {
            JsonValue newSharedState = context.sharedState.copy();
            newSharedState.put(config.scriptResult(), result.get());
            return goToNext().replaceSharedState(newSharedState).build();
        } else {
            Script script = config.script();
            if (script == null || Strings.isNullOrEmpty(script.getScript())) {
                throw new NodeProcessException("No client script has been selected for this node");
            }
            if (script.getLanguage() != ScriptingLanguage.JAVASCRIPT) {
                throw new NodeProcessException("Client-side scripts must be written in JavaScript, but script '"
                        + script.getName() + "' is " + script.getLanguage());
            }
            String clientSideScriptExecutorFunction = createClientSideScriptExecutorFunction(script.getScript(),
                    config.scriptResult(), true, context.sharedState.toString());
            ScriptTextOutputCallback scriptAndSelfSubmitCallback =
                    new ScriptTextOutputCallback(clientSideScriptExecutorFunction);

            HiddenValueCallback hiddenValueCallback = new HiddenValueCallback(config.scriptResult());

            ImmutableList<Callback> callbacks = ImmutableList.of(scriptAndSelfSubmitCallback, hiddenValueCallback);

            return send(callbacks).build();
        }
    }


    public static String createClientSideScriptExecutorFunction(String script, String outputParameterId,
            boolean clientSideScriptEnabled, String context) {
        String collectingDataMessage = "";
        if (clientSideScriptEnabled) {
            collectingDataMessage = "    messenger.messages.addMessage( message );\n";
        }

        String spinningWheelScript = "if (window.require) {\n" +
                "    var messenger = require(\"org/forgerock/commons/ui/common/components/Messages\"),\n" +
                "        spinner =  require(\"org/forgerock/commons/ui/common/main/SpinnerManager\"),\n" +
                "        message =  {message:\"Collecting Data...\", type:\"info\"};\n" +
                "    spinner.showSpinner();\n" +
                collectingDataMessage +
                "}";

        return String.format(
                spinningWheelScript +
                        "(function(output) {\n" +
                        "    var autoSubmitDelay = 0,\n" +
                        "        submitted = false,\n" +
                        "        context = %s;\n" + //injecting context in form of JSON
                        "    function submit() {\n" +
                        "        if (submitted) {\n" +
                        "            return;\n" +
                        "        }" +
                        "        if (!(typeof $ == 'function')) {\n" + // Crude detection to see if XUI is not present.
                        "            document.getElementById('loginButton_0').click();\n" +
                        "        } else {\n" +
                        "            $('input[type=submit]').click();\n" +
                        "        }\n" +
                        "        submitted = true;\n" +
                        "    }\n" +
                        "    %s\n" + // script
                        "    setTimeout(submit, autoSubmitDelay);\n" +
                        "})(document.getElementById('%s'));\n", // outputParameterId
                context,
                script,
                outputParameterId);
    }

}
