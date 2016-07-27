/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.dolezel.jarss.rest.v1;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

/**
 *
 * @author lubos
 */
public class RestApplication extends ResourceConfig {
	
	public RestApplication() {
		register(InitialSetupService.class);
		register(UserService.class);
		register(NoCacheFilter.class);
		register(RolesAllowedDynamicFeature.class);
		register(FeedsService.class);
		register(AuthenticationFilter.class);
		register(FeedCategoryService.class);
	}
}
