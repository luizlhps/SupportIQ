package com.worklyze.supportiq.shared.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This annotation is used to perform searches with the contains statement ignoring cases.
 * For example, if you have a field with the name "name" and you want to search for "john" in the database where the name is like "%john%", you can use this annotation in the name field like this:
 *
 * @ContainsAnnotation
 * private String name;
 *
 * This annotation is useful for searching in fields that can have different formats, like name, email, etc.
 *
 * @author luizlhps
 * @since 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainsAnnotation {
}
