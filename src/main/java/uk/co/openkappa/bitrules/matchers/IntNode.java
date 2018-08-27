package uk.co.openkappa.bitrules.matchers;

import uk.co.openkappa.bitrules.Mask;
import uk.co.openkappa.bitrules.Operation;

import java.lang.reflect.Array;
import java.util.Arrays;

public class IntNode<MaskType extends Mask<MaskType>> {

  private final Operation relation;
  private final MaskType empty;

  private int[] thresholds = new int[16];
  private MaskType[] sets;
  private int count = 0;

  public IntNode(Operation relation, MaskType empty) {
    this.relation = relation;
    this.empty = empty;
    this.sets = (MaskType[]) Array.newInstance(empty.getClass(), 16);
  }

  public void add(int value, int priority) {
    int position = Arrays.binarySearch(thresholds, 0, count, value);
    int insertionPoint = -(position + 1);
    if (position < 0 && insertionPoint < count) {
      incrementCount();
      for (int i = count; i > insertionPoint; --i) {
        sets[i] = sets[i - 1];
        thresholds[i] = thresholds[i - 1];
      }
      sets[insertionPoint] = maskWith(priority);
      thresholds[insertionPoint] = value;
    } else if (position < 0) {
      sets[count] = maskWith(priority);
      thresholds[count] = value;
      incrementCount();
    } else {
      sets[position].add(priority);
    }
  }

  public MaskType apply(int value, MaskType context) {
    switch (relation) {
      case GT:
        return context.inPlaceAnd(findRangeEncoded(value));
      case GE:
        return context.inPlaceAnd(findRangeEncodedInclusive(value));
      case LT:
        return context.inPlaceAnd(findReverseRangeEncoded(value));
      case LE:
        return context.inPlaceAnd(findReverseRangeEncodedInclusive(value));
      case EQ:
        return context.inPlaceAnd(findEqualityEncoded(value));
      default:
        return context;
    }
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
    return index >= 0 ? sets[index] : empty;
  }

  private MaskType findRangeEncoded(int value) {
    int pos = Arrays.binarySearch(thresholds, 0, count, value);
    int index = (pos >= 0 ? pos : -(pos + 1)) - 1;
    return index >= 0 && index < count ? sets[index] : empty;
  }

  private MaskType findRangeEncodedInclusive(int value) {
    int pos = Arrays.binarySearch(thresholds, 0, count, value);
    int index = (pos >= 0 ? pos : -(pos + 1) - 1);
    return index >= 0 && index < count ? sets[index] : empty;
  }

  private MaskType findReverseRangeEncoded(int value) {
    int pos = Arrays.binarySearch(thresholds, 0, count, value);
    int index = (pos >= 0 ? pos + 1 : -(pos + 1));
    return index >= 0 && index < count ? sets[index] : empty;
  }

  private MaskType findReverseRangeEncodedInclusive(int value) {
    int pos = Arrays.binarySearch(thresholds, 0, count, value);
    int index = (pos >= 0 ? pos : -(pos + 1));
    return index >= 0 && index < count ? sets[index] : empty;
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

  private void incrementCount() {
    ++count;
    if (count == thresholds.length) {
      sets = Arrays.copyOf(sets, count * 2);
      thresholds = Arrays.copyOf(thresholds, count * 2);
    }
  }

  private MaskType maskWith(int value) {
    MaskType mask = empty.clone();
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