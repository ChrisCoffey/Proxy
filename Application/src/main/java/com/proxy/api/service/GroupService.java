package com.proxy.api.service;

import com.proxy.api.model.Group;
import com.proxy.api.model.User;

import java.util.Map;

import retrofit.http.GET;
import retrofit.http.Path;
import rx.Observable;

/**
 * Group services for {@link User}s.
 */
public interface GroupService {

    /**
     * Get a {@link User}'s {@link Group}s.
     *
     * @param userId unique id for {@link User} table
     */
    @GET("/users/{userId}/Groups.json")
    Observable<Map<String, Group>> getUserGroups(@Path("userId") String userId);
}