package com.openkappa.bitrules.nodes;

import com.openkappa.bitrules.DoubleRelation;
import org.junit.Test;
import org.roaringbitmap.ArrayContainer;
import org.roaringbitmap.Container;
import org.roaringbitmap.RunContainer;

import static org.junit.Assert.*;

public class DoubleNodeTest {
  @Test
  public void testGreaterThan() {
    DoubleNode node = build(100, DoubleRelation.GT);
    Container mask = RunContainer.rangeOfOnes(0, 100);
    assertTrue(node.apply(0, mask.clone()).isEmpty());
    assertEquals(new ArrayContainer().add((short) 0), node.apply(1, mask.clone()));
  }

  @Test
  public void testEqual() {
    DoubleNode node = build(100, DoubleRelation.EQ);
    Container mask = RunContainer.rangeOfOnes(0, 100);
    assertTrue(node.apply(-1, mask.clone()).isEmpty());
    assertEquals(new ArrayContainer().add((short) 0), node.apply(0, mask.clone()));
    assertEquals(new ArrayContainer().add((short) 1), node.apply(10, mask.clone()));
  }

  @Test
  public void testLessThan() {
    DoubleNode node = build(100, DoubleRelation.LT);
    Container mask = RunContainer.rangeOfOnes(0, 100);
    assertTrue(node.apply(1001, mask.clone()).isEmpty());
    assertEquals(mask.clone().remove((short) 0), node.apply(0, mask.clone()));
    assertEquals(mask.clone().remove((short) 0).remove((short) 1), node.apply(10, mask.clone()));
  }

  @Test
  public void testGreaterThanRev() {
    DoubleNode node = buildRev(100, DoubleRelation.GT);
    Container mask = RunContainer.rangeOfOnes(0, 100);
    assertTrue(node.apply(0, mask.clone()).isEmpty());
    assertEquals(new ArrayContainer().add((short) 0), node.apply(1, mask.clone()));
  }

  @Test
  public void testEqualRev() {
    DoubleNode node = buildRev(100, DoubleRelation.EQ);
    Container mask = RunContainer.rangeOfOnes(0, 100);
    assertTrue(node.apply(-1, mask.clone()).isEmpty());
    assertEquals(new ArrayContainer().add((short) 0), node.apply(0, mask.clone()));
    assertEquals(new ArrayContainer().add((short) 1), node.apply(10, mask.clone()));
  }

  @Test
  public void testLessThanRev() {
    DoubleNode node = buildRev(100, DoubleRelation.LT);
    Container mask = RunContainer.rangeOfOnes(0, 100);
    assertTrue(node.apply(1001, mask.clone()).isEmpty());
    assertEquals(mask.clone().remove((short) 0), node.apply(0, mask.clone()));
    assertEquals(mask.clone().remove((short) 0).remove((short) 1), node.apply(10, mask.clone()));
  }

  @Test
  public void testBuildNode() {
    DoubleNode node = new DoubleNode(DoubleRelation.EQ);
    node.add(0, (short)0);
    assertEquals(RunContainer.rangeOfOnes(0, 1), node.apply(0, RunContainer.rangeOfOnes(0, 1)));
    node.add(0, (short)1);
    assertEquals(RunContainer.rangeOfOnes(0, 2), node.apply(0, RunContainer.rangeOfOnes(0, 2)));
  }

  private DoubleNode build(int count, DoubleRelation relation) {
    DoubleNode node = new DoubleNode(relation);
    for (int i = 0; i < count; ++i) {
      node.add(i * 10, (short) i);
    }
    return node;
  }

  private DoubleNode buildRev(int count, DoubleRelation relation) {
    DoubleNode node = new DoubleNode(relation);
    for (int i = count - 1; i >= 0; --i) {
      node.add(i * 10, (short) i);
    }
    return node;
  }
}