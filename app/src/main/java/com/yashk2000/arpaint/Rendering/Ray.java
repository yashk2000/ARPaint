package com.yashk2000.arpaint.Rendering;

import javax.vecmath.Vector3f;

public class Ray {
  public final Vector3f origin;
  public final Vector3f direction;

  public Ray(Vector3f origin, Vector3f direction) {
    this.origin = origin;
    this.direction = direction;
  }
}
