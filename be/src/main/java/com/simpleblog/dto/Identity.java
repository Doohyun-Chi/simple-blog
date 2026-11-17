package com.simpleblog.dto;

/** Who is signed in, as in be/apispec.yaml: the email of the verified Google ID token. */
public record Identity(String email) {
}
