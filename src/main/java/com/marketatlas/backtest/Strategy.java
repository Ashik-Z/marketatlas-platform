package com.marketatlas.backtest;

import java.util.List;

/** Decides, after the close of bars[index], whether to be invested. Only bars up to index may be used. */
public interface Strategy {

    boolean wantsPosition(List<Backtest.Bar> bars, int index);

    String name();
}