package app.michaelwuensch.bitbanana.backends;

public class RestErrorResponse {

    private boolean error;
    private String message;
    private String detail;

    public boolean getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getDetail() {
        return detail;
    }
}
