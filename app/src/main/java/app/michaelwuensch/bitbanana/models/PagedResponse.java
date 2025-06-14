package app.michaelwuensch.bitbanana.models;

import java.io.Serializable;
import java.util.List;

public class PagedResponse<T> implements Serializable {

    private final List<T> Page;
    private final long FirstIndexOffset;
    private final long LastIndexOffset;
    private final int OriginalBackendPageSize;

    public static <T> Builder<T> newBuilder() {
        return new Builder<>();
    }

    private PagedResponse(Builder<T> builder) {
        this.Page = builder.Page;
        this.FirstIndexOffset = builder.FirstIndexOffset;
        this.LastIndexOffset = builder.LastIndexOffset;
        this.OriginalBackendPageSize = builder.OriginalBackendPageSize;
    }

    public List<T> getPage() {
        return Page;
    }

    public long getFirstIndexOffset() {
        return FirstIndexOffset;
    }

    public long getLastIndexOffset() {
        return LastIndexOffset;
    }

    /**
     * It is important to use this instead of getPage.getSize() when we check for the ending condition in batched calls.
     * The reason is that in some case we want to filter out some of the returned results which would lead to termination of the queue.
     * Using this (and setting it correctly before) we can still filter out results and continue until we have iterated over all items.
     */
    public int getOriginalBackendPageSize() {
        return OriginalBackendPageSize;
    }

    // Builder Class
    public static class Builder<T> {

        private List<T> Page;
        private long FirstIndexOffset;
        private long LastIndexOffset;
        private int OriginalBackendPageSize;

        private Builder() {
            // required parameters
        }

        public PagedResponse<T> build() {
            return new PagedResponse<>(this);
        }

        public Builder<T> setPage(List<T> page) {
            Page = page;
            return this;
        }

        public Builder<T> setFirstIndexOffset(long firstIndexOffset) {
            FirstIndexOffset = firstIndexOffset;
            return this;
        }

        public Builder<T> setLastIndexOffset(long lastIndexOffset) {
            LastIndexOffset = lastIndexOffset;
            return this;
        }

        /**
         * Set this to the number of elements returned by the backend.
         * It is important that if BitBanana further filters the results, this value still contains the original number from the backend.
         */
        public Builder<T> setOriginalBackendPageSize(int originalBackendPageSize) {
            OriginalBackendPageSize = originalBackendPageSize;
            return this;
        }
    }
}