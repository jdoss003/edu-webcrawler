package com.bdj.eduwebcrawler;

import java.util.Objects;

public class Pair<A, B>
{
    public final A fst;
    public final B snd;

    public Pair(A fst, B snd)
    {
        this.fst = fst;
        this.snd = snd;
    }

    public static <A, B> Pair<A, B> of(A fst, B snd)
    {
        return new Pair<>(fst, snd);
    }

    public boolean equals(Object o)
    {
        return o instanceof Pair && Objects.equals(this.fst, ((Pair)o).fst) && Objects.equals(this.snd, ((Pair)o).snd);
    }
}
