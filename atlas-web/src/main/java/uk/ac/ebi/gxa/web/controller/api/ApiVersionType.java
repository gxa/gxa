package uk.ac.ebi.gxa.web.controller.api;

/**
 * This class stores an enumeration of valid version types for curation api calls.
 */
public enum ApiVersionType {
    /**
     * JSON/XML download buttons - change with caution
     */
    vx,
    /**
     * internal code - we are free to change this api (mapping to another handler/controller, obviously)
     */
    //
    v0,
    /**
     * External API - public contract, we MUST NOT remove calls or change response format.
     * Adding calls is pretty much fine, but think twice before you add - it'll be painful to change later
     */
    v1
}

