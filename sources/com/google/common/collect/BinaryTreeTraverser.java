package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;
import java.util.Iterator;

@GwtCompatible(emulated = true)
@Beta
public abstract class BinaryTreeTraverser<T> extends TreeTraverser<T> {

    private final class PostOrderIterator extends UnmodifiableIterator<T> {
        private final BitSet hasExpanded;
        private final Deque<T> stack = new ArrayDeque();

        PostOrderIterator(T root) {
            this.stack.addLast(root);
            this.hasExpanded = new BitSet();
        }

        public boolean hasNext() {
            return this.stack.isEmpty() ^ 1;
        }

        public T next() {
            while (true) {
                T node = this.stack.getLast();
                if (this.hasExpanded.get(this.stack.size() - 1)) {
                    this.stack.removeLast();
                    this.hasExpanded.clear(this.stack.size());
                    return node;
                }
                this.hasExpanded.set(this.stack.size() - 1);
                BinaryTreeTraverser.pushIfPresent(this.stack, BinaryTreeTraverser.this.rightChild(node));
                BinaryTreeTraverser.pushIfPresent(this.stack, BinaryTreeTraverser.this.leftChild(node));
            }
        }
    }

    private final class PreOrderIterator extends UnmodifiableIterator<T> implements PeekingIterator<T> {
        private final Deque<T> stack = new ArrayDeque();

        PreOrderIterator(T root) {
            this.stack.addLast(root);
        }

        public boolean hasNext() {
            return this.stack.isEmpty() ^ 1;
        }

        public T next() {
            T result = this.stack.removeLast();
            BinaryTreeTraverser.pushIfPresent(this.stack, BinaryTreeTraverser.this.rightChild(result));
            BinaryTreeTraverser.pushIfPresent(this.stack, BinaryTreeTraverser.this.leftChild(result));
            return result;
        }

        public T peek() {
            return this.stack.getLast();
        }
    }

    private final class InOrderIterator extends AbstractIterator<T> {
        private final BitSet hasExpandedLeft = new BitSet();
        private final Deque<T> stack = new ArrayDeque();

        InOrderIterator(T root) {
            this.stack.addLast(root);
        }

        /* Access modifiers changed, original: protected */
        public T computeNext() {
            while (!this.stack.isEmpty()) {
                T node = this.stack.getLast();
                if (this.hasExpandedLeft.get(this.stack.size() - 1)) {
                    this.stack.removeLast();
                    this.hasExpandedLeft.clear(this.stack.size());
                    BinaryTreeTraverser.pushIfPresent(this.stack, BinaryTreeTraverser.this.rightChild(node));
                    return node;
                }
                this.hasExpandedLeft.set(this.stack.size() - 1);
                BinaryTreeTraverser.pushIfPresent(this.stack, BinaryTreeTraverser.this.leftChild(node));
            }
            return endOfData();
        }
    }

    public abstract Optional<T> leftChild(T t);

    public abstract Optional<T> rightChild(T t);

    public final Iterable<T> children(final T root) {
        Preconditions.checkNotNull(root);
        return new FluentIterable<T>() {
            public Iterator<T> iterator() {
                return new AbstractIterator<T>() {
                    boolean doneLeft;
                    boolean doneRight;

                    /* Access modifiers changed, original: protected */
                    public T computeNext() {
                        Optional<T> left;
                        if (!this.doneLeft) {
                            this.doneLeft = true;
                            left = BinaryTreeTraverser.this.leftChild(root);
                            if (left.isPresent()) {
                                return left.get();
                            }
                        }
                        if (!this.doneRight) {
                            this.doneRight = true;
                            left = BinaryTreeTraverser.this.rightChild(root);
                            if (left.isPresent()) {
                                return left.get();
                            }
                        }
                        return endOfData();
                    }
                };
            }
        };
    }

    /* Access modifiers changed, original: 0000 */
    public UnmodifiableIterator<T> preOrderIterator(T root) {
        return new PreOrderIterator(root);
    }

    /* Access modifiers changed, original: 0000 */
    public UnmodifiableIterator<T> postOrderIterator(T root) {
        return new PostOrderIterator(root);
    }

    public final FluentIterable<T> inOrderTraversal(final T root) {
        Preconditions.checkNotNull(root);
        return new FluentIterable<T>() {
            public UnmodifiableIterator<T> iterator() {
                return new InOrderIterator(root);
            }
        };
    }

    private static <T> void pushIfPresent(Deque<T> stack, Optional<T> node) {
        if (node.isPresent()) {
            stack.addLast(node.get());
        }
    }
}
