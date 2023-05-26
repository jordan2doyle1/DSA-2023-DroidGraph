package phd.research;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class Pair<L, R> {

    @NotNull
    private final L left;
    @NotNull
    private final R right;

    public Pair(L left, R right) {
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
    }

    @NotNull
    public L getLeft() {
        return this.left;
    }

    @NotNull
    public R getRight() {
        return this.right;
    }

    @Override
    public String toString() {
        return String.format("%s: (%s, %s)", getClass().getSimpleName(), this.left, this.right);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Pair<?, ?>) {
            return Objects.equals(((Pair<?, ?>) obj).left, this.left) &&
                    Objects.equals(((Pair<?, ?>) obj).right, this.right);
        }

        return false;
    }

    @Override
    public final int hashCode() {
        return this.left.hashCode() + this.right.hashCode();
    }
}
