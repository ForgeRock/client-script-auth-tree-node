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
 * Copyright 2021-2022 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.clientscript;

// import static org.forgerock.openam.auth.nodes.clientscript.AuthNodesGlobalScript.CONFIG_PROVIDER_NODE_SCRIPT;
// import static org.forgerock.openam.auth.nodes.clientscript.AuthNodesGlobalScript.DECISION_NODE_SCRIPT;
import static org.forgerock.openam.auth.nodes.clientscript.AuthNodesScriptContext.AUTHENTICATION_CLIENT_SIDE;
// import static org.forgerock.openam.auth.nodes.clientscript.AuthNodesScriptContext.CONFIG_PROVIDER_NODE;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.scripting.domain.ScriptContextDetails;
import org.forgerock.openam.scripting.persistence.config.defaults.AnnotatedServiceRegistryScriptContextDetailsProvider;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;

import com.google.inject.Inject;

/**
 * Responsible for providing the auth node script contexts.
 */
public class AuthNodesScriptContextProvider extends AnnotatedServiceRegistryScriptContextDetailsProvider {



    @Inject
    AuthNodesScriptContextProvider(AnnotatedServiceRegistry annotatedServiceRegistry) {
        super(annotatedServiceRegistry);
    }

    @Override
    public List<ScriptContextDetails> get() {
        List<ScriptContextDetails> scriptContexts = new ArrayList<>();

        scriptContexts.add(ScriptContextDetails.builder()
                .withContextReference(AUTHENTICATION_CLIENT_SIDE)
                .withI18NKey("script-type-03")
                .build());


        return scriptContexts;
    }
}
