/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */
package org.wso2.carbon.andes.authentication.internal;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.andes.configuration.AndesConfigurationManager;
import org.wso2.andes.kernel.AndesException;
import org.wso2.carbon.andes.authentication.andes.oauth.config.OAuthConfigurationManager;
import org.wso2.carbon.andes.authentication.service.AuthenticationService;
import org.wso2.carbon.andes.authentication.service.AuthenticationServiceImpl;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
        name = "org.wso2.carbon.andes.authentication.internal.AuthenticationServiceComponent",
        immediate = true)
public class AuthenticationServiceComponent {

    private ServiceRegistration authenticationService = null;

    private static final String CARBON_CONFIG_PORT_OFFSET = "Ports.Offset";

    private static final int CARBON_DEFAULT_PORT_OFFSET = 0;

    @Activate
    protected void activate(ComponentContext ctx) throws AndesException {
        // TODO :reinitializing Andes Configuration manager, since we cannot guarantee the startup order, have to
        // provide proper configuration service
        AndesConfigurationManager.initialize(getPortOffset());
        OAuthConfigurationManager.getInstance().initConfig();
        // Generate access key
        String accessKey = UUID.randomUUID().toString();
        AuthenticationServiceDataHolder.getInstance().setAccessKey(accessKey);
        // Publish access key
        authenticationService = ctx.getBundleContext().registerService(AuthenticationService.class.getName(), new
                AuthenticationServiceImpl(accessKey), null);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        // Unregister AuthenticationService
        if (null != authenticationService) {
            authenticationService.unregister();
        }
    }

    @Reference(
            name = "registry.service",
            service = org.wso2.carbon.registry.core.service.RegistryService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRegistryService")
    protected void setRegistryService(RegistryService registryService) {

        AuthenticationServiceDataHolder.getInstance().setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {

        AuthenticationServiceDataHolder.getInstance().setRegistryService(null);
    }

    @Reference(
            name = "realm.service",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        AuthenticationServiceDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        AuthenticationServiceDataHolder.getInstance().setRealmService(null);
    }

    // wait till serverConfigurationService is started to pick the carbon offset
    @Reference(
            name = "server.configuration",
            service = org.wso2.carbon.base.api.ServerConfigurationService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetServerConfiguration")
    protected void setServerConfiguration(ServerConfigurationService serverConfiguration) {

    }

    protected void unsetServerConfiguration(ServerConfigurationService serverConfiguration) {

    }

    private int getPortOffset() {

        ServerConfiguration carbonConfig = ServerConfiguration.getInstance();
        String portOffset = System.getProperty("portOffset", carbonConfig.getFirstProperty(CARBON_CONFIG_PORT_OFFSET));
        try {
            if ((portOffset != null)) {
                return Integer.parseInt(portOffset.trim());
            } else {
                return CARBON_DEFAULT_PORT_OFFSET;
            }
        } catch (NumberFormatException e) {
            return CARBON_DEFAULT_PORT_OFFSET;
        }
    }
}
