package io.github.richardstartin.multimatcher.core.matchers.nodes;

import io.github.richardstartin.multimatcher.core.Mask;
import io.github.richardstartin.multimatcher.core.Operation;
import io.github.richardstartin.multimatcher.core.masks.MaskFactory;

import java.lang.reflect.Array;
import java.util.Arrays;

import static io.github.richardstartin.multimatcher.core.matchers.SelectivityHeuristics.avgCardinality;

public class IntNode<MaskType extends Mask<MaskType>> {

    private final Operation relation;
    private final MaskFactory<MaskType> factory;

    private int[] thresholds = new int[4];
    private MaskType[] sets;
    private int count = 0;

    @SuppressWarnings("unchecked")
    public IntNode(MaskFactory<MaskType> factory, Operation relation) {
        this.relation = relation;
        this.factory = factory;
        this.sets = (MaskType[]) Array.newInstance(factory.emptySingleton().getClass(), 4);
    }

    public void add(int value, int priority) {
        if (count > 0 && value > thresholds[count - 1]) {
            ensureCapacity();
            int position = count;
            MaskType mask = sets[position];
            if (null == mask) {
                mask = factory.newMask();
            }
            mask.add(priority);
            thresholds[position] = value;
            sets[position] = mask;
            ++count;
        } else {
            int position = Arrays.binarySearch(thresholds, 0, count, value);
            int insertionPoint = -(position + 1);
            if (position < 0 && insertionPoint < count) {
                ensureCapacity();
                for (int i = count; i > insertionPoint; --i) {
                    sets[i] = sets[i - 1];
                    thresholds[i] = thresholds[i - 1];
                }
                sets[insertionPoint] = maskWith(priority);
                thresholds[insertionPoint] = value;
                ++count;
            } else if (position < 0) {
                ensureCapacity();
                sets[count] = maskWith(priority);
                thresholds[count] = value;
                ++count;
            } else {
                sets[position].add(priority);
            }
        }
    }

    public MaskType apply(int value, MaskType defaultValue) {
        switch (relation) {
            case GT:
                return findRangeEncoded(value);
            case GE:
                return findRangeEncodedInclusive(value);
            case LT:
                return findReverseRangeEncoded(value);
            case LE:
                return findReverseRangeEncodedInclusive(value);
            case EQ:
                return findEqualityEncoded(value);
            default:
                return defaultValue;
        }
    }


    public float averageSelectivity() {
        return avgCardinality(sets);
    }

    public IntNode<MaskType> optimise() {
        switch (relation) {
            case GE:
            case GT:
                rangeEncode();
                break;
            case LE:
            case LT:
                reverseRangeEncode();
                break;
            default:
        }
        trim();
        return this;
    }

    private MaskType findEqualityEncoded(int value) {
        int index = Arrays.binarySearch(thresholds, 0, count, value);
        return index >= 0 ? sets[index] : factory.emptySingleton();
    }

    private MaskType findRangeEncoded(int value) {
        int pos = Arrays.binarySearch(thresholds, 0, count, value);
        int index = (pos >= 0 ? pos : -(pos + 1)) - 1;
        return index >= 0 && index < count ? sets[index] : factory.emptySingleton();
    }

    private MaskType findRangeEncodedInclusive(int value) {
        int pos = Arrays.binarySearch(thresholds, 0, count, value);
        int index = (pos >= 0 ? pos : -(pos + 1) - 1);
        return index >= 0 && index < count ? sets[index] : factory.emptySingleton();
    }

    private MaskType findReverseRangeEncoded(int value) {
        int pos = Arrays.binarySearch(thresholds, 0, count, value);
        int index = (pos >= 0 ? pos + 1 : -(pos + 1));
        return index >= 0 && index < count ? sets[index] : factory.emptySingleton();
    }

    private MaskType findReverseRangeEncodedInclusive(int value) {
        int pos = Arrays.binarySearch(thresholds, 0, count, value);
        int index = (pos >= 0 ? pos : -(pos + 1));
        return index < count ? sets[index] : factory.emptySingleton();
    }

    private void reverseRangeEncode() {
        for (int i = count - 2; i >= 0; --i) {
            sets[i].inPlaceOr(sets[i + 1]).optimise();
        }
    }

    private void rangeEncode() {
        for (int i = 1; i < count; ++i) {
            sets[i].inPlaceOr(sets[i - 1]).optimise();
        }
    }

    private void trim() {
        sets = Arrays.copyOf(sets, count);
        thresholds = Arrays.copyOf(thresholds, count);
    }

    private void ensureCapacity() {
        int newCount = count + 1;
        if (newCount == thresholds.length) {
            sets = Arrays.copyOf(sets, newCount * 2);
            thresholds = Arrays.copyOf(thresholds, newCount * 2);
        }
    }

    private MaskType maskWith(int value) {
        MaskType mask = factory.newMask();
        mask.add(value);
        return mask;
    }

    @Override
    public String toString() {
        return Nodes.toString(count, relation,
                Arrays.stream(thresholds).boxed().iterator(),
                Arrays.stream(sets).iterator());
    }
}
