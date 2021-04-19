package com.tahitiste.grpc;

import java.util.Optional;

public class Either<T, U> {
    private final T left;
    private final U right;

    private Either(T left, U right) {
        this.left = left;
        this.right = right;
    }

    public static <T, U> Either<T, U> left(T left) {
        return new Either<>(left, null);
    }

    public static <T, U> Either<T, U> right(U right) {
        return new Either<>(null, right);
    }

    public Optional<T> getLeft() {
        return Optional.ofNullable(left);
    }

    public Optional<U> getRight() {
        return Optional.ofNullable(right);
    }
}
