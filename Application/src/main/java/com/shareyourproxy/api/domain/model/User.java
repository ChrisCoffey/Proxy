package com.shareyourproxy.api.domain.model;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.shareyourproxy.api.gson.AutoGson;

import java.util.HashMap;

import auto.parcel.AutoParcel;

/**
 * Users have a basic profile that contains their specific {@link Channel}s, {@link Contact}s, and
 * {@link Group}s.
 */
@AutoParcel
@AutoGson(autoValueClass = AutoParcel_User.class)
public abstract class User implements Parcelable {

    /**
     * User Constructor.
     *
     * @param id         user unique ID
     * @param firstName  user first name
     * @param lastName   user last name
     * @param email      user email
     * @param profileURL user profile picture
     * @param coverURL   user cover image
     * @param channels   user channels
     * @param groups     user contactGroups
     * @param contacts   user contacts
     * @param messages   user messages
     * @return the entered user data
     */
    public static User create(
        Id id, String firstName, String lastName, String email, String profileURL,
        String coverURL, HashMap<String, Channel> channels, HashMap<String, Group> groups,
        HashMap<String, Contact> contacts, HashMap<String, Message> messages) {
        return builder().id(id).first(firstName).last(lastName).email(email)
            .profileURL(profileURL).coverURL(coverURL).channels(channels)
            .groups(groups).contacts(contacts).messages(messages).build();
    }

    /**
     * User builder.
     *
     * @return this User.
     */
    public static Builder builder() {
        // The subclass AutoParcel_PackagelessValueType is created by the annotation processor
        // that is triggered by the presence of the @AutoParcel annotation. It has a constructor
        // for each of the abstract getter methods here, in order. The constructor stashes the
        // values here in private final fields, and each method is implemented to return the
        // value of the corresponding field.
        return new AutoParcel_User.Builder();
    }

    /**
     * Get users unique ID.
     *
     * @return first name
     */
    public abstract Id id();

    /**
     * Get users first name.
     *
     * @return first name
     */
    public abstract String first();

    /**
     * Get users last name.
     *
     * @return last name
     */
    public abstract String last();

    /**
     * Get users email.
     *
     * @return email
     */
    @Nullable
    public abstract String email();


    /**
     * Get user profile image.
     *
     * @return profile image
     */
    public abstract String profileURL();

    /**
     * Get user profile image.
     *
     * @return profile image
     */
    @Nullable
    public abstract String coverURL();

    /**
     * Get users channels.
     *
     * @return channels
     */
    @Nullable
    public abstract HashMap<String, Channel> channels();

    /**
     * Get users contacts.
     *
     * @return contacts
     */
    @Nullable
    public abstract HashMap<String, Contact> contacts();

    /**
     * Get users contactGroups.
     *
     * @return contactGroups
     */
    @Nullable
    public abstract HashMap<String, Group> groups();

    /**
     * Get users messages.
     *
     * @return messages
     */
    @Nullable
    public abstract HashMap<String, Message> messages();

    /**
     * Validation conditions.
     */
    @AutoParcel.Validate
    public void validate() {
        if (first().length() == 0 || last() == null) {
            throw new IllegalStateException("Need a valid first name");
        }
        if (last().length() == 0 || last() == null) {
            throw new IllegalStateException("Need a valid last name");
        }
    }


    /**
     * User Builder.
     */
    @AutoParcel.Builder
    public interface Builder {

        /**
         * Set user id.
         *
         * @param id user unqiue id
         * @return user id
         */
        Builder id(Id id);

        /**
         * Set user first name.
         *
         * @param firstName user first name
         * @return first name string
         */
        Builder first(String firstName);

        /**
         * Set users last name.
         *
         * @param lastName user last name
         * @return last name string
         */
        Builder last(String lastName);

        /**
         * Set user email.
         *
         * @param email this email
         * @return email string
         */
        Builder email(String email);

        /**
         * Set the user profile image URL.
         *
         * @param profileURL profile image url
         * @return URL string
         */
        Builder profileURL(String profileURL);

        /**
         * Set the user profile image URL.
         *
         * @param coverURL profile cover url
         * @return URL string
         */
        @Nullable
        Builder coverURL(String coverURL);

        /**
         * Set this {@link User}s {@link Contact}s
         *
         * @param contacts user contacts
         * @return List {@link Contact}
         */
        @Nullable
        Builder contacts(HashMap<String, Contact> contacts);

        /**
         * Set this {@link User}s {@link Group}s
         *
         * @param groups user contactGroups
         * @return List {@link Group}
         */
        @Nullable
        Builder groups(HashMap<String, Group> groups);

        /**
         * Set this {@link User}s {@link Channel}s
         *
         * @param channels user channels
         * @return List {@link Channel}
         */
        @Nullable
        Builder channels(HashMap<String, Channel> channels);

        /**
         * Set this {@link User}s {@link Message}s
         *
         * @param messages user messages
         * @return List {@link Channel}
         */
        @Nullable
        Builder messages(HashMap<String, Message> messages);

        /**
         * BUILD.
         *
         * @return User
         */
        User build();
    }

}
