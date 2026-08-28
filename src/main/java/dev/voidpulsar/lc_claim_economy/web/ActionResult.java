package dev.voidpulsar.lc_claim_economy.web;

/** Plain result of a dashboard mutating action - deliberately backend-agnostic, see {@link WebDataService}'s javadoc. */
record ActionResult(boolean success, String message) {
    static ActionResult success(String message) {
        return new ActionResult(true, message);
    }

    static ActionResult failure(String message) {
        return new ActionResult(false, message);
    }
}
