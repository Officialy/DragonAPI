package reika.dragonapi.instantiable.math;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

import java.io.Serializable;
import java.nio.DoubleBuffer;

/**
 * A double-based copy of LWJGL's {@link org.joml.Matrix4f} class, for use with doubles and in environments without LWJGL libraries available.
 * <p>
 * ==========================================================================================
 * <p>
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * <p>
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * <p>
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * <p>
 * * Neither the name of 'LWJGL' nor the names of
 * its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class DoubleMatrix implements Serializable {

    public double m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33;

    /**
     * Construct a new matrix, initialized to the identity.
     */
    public DoubleMatrix() {
        super();
        this.setIdentity();
    }

    public DoubleMatrix(final DoubleMatrix src) {
        super();
        this.load(src);
    }

    /**
     * Set the given matrix to be the identity matrix.
     *
     * @param m The matrix to set to the identity
     * @return m
     */
    public static DoubleMatrix setIdentity(DoubleMatrix m) {
        m.m00 = 1.0f;
        m.m01 = 0.0f;
        m.m02 = 0.0f;
        m.m03 = 0.0f;
        m.m10 = 0.0f;
        m.m11 = 1.0f;
        m.m12 = 0.0f;
        m.m13 = 0.0f;
        m.m20 = 0.0f;
        m.m21 = 0.0f;
        m.m22 = 1.0f;
        m.m23 = 0.0f;
        m.m30 = 0.0f;
        m.m31 = 0.0f;
        m.m32 = 0.0f;
        m.m33 = 1.0f;

        return m;
    }

    /**
     * Set the given matrix to 0.
     *
     * @param m The matrix to set to 0
     * @return m
     */
    public static DoubleMatrix setZero(DoubleMatrix m) {
        m.m00 = 0.0f;
        m.m01 = 0.0f;
        m.m02 = 0.0f;
        m.m03 = 0.0f;
        m.m10 = 0.0f;
        m.m11 = 0.0f;
        m.m12 = 0.0f;
        m.m13 = 0.0f;
        m.m20 = 0.0f;
        m.m21 = 0.0f;
        m.m22 = 0.0f;
        m.m23 = 0.0f;
        m.m30 = 0.0f;
        m.m31 = 0.0f;
        m.m32 = 0.0f;
        m.m33 = 0.0f;

        return m;
    }

    /**
     * Copy the source matrix to the destination matrix
     *
     * @param src  The source matrix
     * @param dest The destination matrix, or null of a new one is to be created
     * @return The copied matrix
     */
    public static DoubleMatrix load(DoubleMatrix src, DoubleMatrix dest) {
        if (dest == null)
            dest = new DoubleMatrix();
        dest.m00 = src.m00;
        dest.m01 = src.m01;
        dest.m02 = src.m02;
        dest.m03 = src.m03;
        dest.m10 = src.m10;
        dest.m11 = src.m11;
        dest.m12 = src.m12;
        dest.m13 = src.m13;
        dest.m20 = src.m20;
        dest.m21 = src.m21;
        dest.m22 = src.m22;
        dest.m23 = src.m23;
        dest.m30 = src.m30;
        dest.m31 = src.m31;
        dest.m32 = src.m32;
        dest.m33 = src.m33;

        return dest;
    }

    /**
     * Add two matrices together and place the result in a third matrix.
     *
     * @param left  The left source matrix
     * @param right The right source matrix
     * @param dest  The destination matrix, or null if a new one is to be created
     * @return the destination matrix
     */
    public static DoubleMatrix add(DoubleMatrix left, DoubleMatrix right, DoubleMatrix dest) {
        if (dest == null)
            dest = new DoubleMatrix();

        dest.m00 = left.m00 + right.m00;
        dest.m01 = left.m01 + right.m01;
        dest.m02 = left.m02 + right.m02;
        dest.m03 = left.m03 + right.m03;
        dest.m10 = left.m10 + right.m10;
        dest.m11 = left.m11 + right.m11;
        dest.m12 = left.m12 + right.m12;
        dest.m13 = left.m13 + right.m13;
        dest.m20 = left.m20 + right.m20;
        dest.m21 = left.m21 + right.m21;
        dest.m22 = left.m22 + right.m22;
        dest.m23 = left.m23 + right.m23;
        dest.m30 = left.m30 + right.m30;
        dest.m31 = left.m31 + right.m31;
        dest.m32 = left.m32 + right.m32;
        dest.m33 = left.m33 + right.m33;

        return dest;
    }

    /**
     * Subtract the right matrix from the left and place the result in a third matrix.
     *
     * @param left  The left source matrix
     * @param right The right source matrix
     * @param dest  The destination matrix, or null if a new one is to be created
     * @return the destination matrix
     */
    public static DoubleMatrix sub(DoubleMatrix left, DoubleMatrix right, DoubleMatrix dest) {
        if (dest == null)
            dest = new DoubleMatrix();

        dest.m00 = left.m00 - right.m00;
        dest.m01 = left.m01 - right.m01;
        dest.m02 = left.m02 - right.m02;
        dest.m03 = left.m03 - right.m03;
        dest.m10 = left.m10 - right.m10;
        dest.m11 = left.m11 - right.m11;
        dest.m12 = left.m12 - right.m12;
        dest.m13 = left.m13 - right.m13;
        dest.m20 = left.m20 - right.m20;
        dest.m21 = left.m21 - right.m21;
        dest.m22 = left.m22 - right.m22;
        dest.m23 = left.m23 - right.m23;
        dest.m30 = left.m30 - right.m30;
        dest.m31 = left.m31 - right.m31;
        dest.m32 = left.m32 - right.m32;
        dest.m33 = left.m33 - right.m33;

        return dest;
    }

    /**
     * Multiply the right matrix by the left and place the result in a third matrix.
     *
     * @param left  The left source matrix
     * @param right The right source matrix
     * @param dest  The destination matrix, or null if a new one is to be created
     * @return the destination matrix
     */
    public static DoubleMatrix mul(DoubleMatrix left, DoubleMatrix right, DoubleMatrix dest) {
        if (dest == null)
            dest = new DoubleMatrix();

        double m00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02 + left.m30 * right.m03;
        double m01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02 + left.m31 * right.m03;
        double m02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02 + left.m32 * right.m03;
        double m03 = left.m03 * right.m00 + left.m13 * right.m01 + left.m23 * right.m02 + left.m33 * right.m03;
        double m10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12 + left.m30 * right.m13;
        double m11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12 + left.m31 * right.m13;
        double m12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12 + left.m32 * right.m13;
        double m13 = left.m03 * right.m10 + left.m13 * right.m11 + left.m23 * right.m12 + left.m33 * right.m13;
        double m20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22 + left.m30 * right.m23;
        double m21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22 + left.m31 * right.m23;
        double m22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22 + left.m32 * right.m23;
        double m23 = left.m03 * right.m20 + left.m13 * right.m21 + left.m23 * right.m22 + left.m33 * right.m23;
        double m30 = left.m00 * right.m30 + left.m10 * right.m31 + left.m20 * right.m32 + left.m30 * right.m33;
        double m31 = left.m01 * right.m30 + left.m11 * right.m31 + left.m21 * right.m32 + left.m31 * right.m33;
        double m32 = left.m02 * right.m30 + left.m12 * right.m31 + left.m22 * right.m32 + left.m32 * right.m33;
        double m33 = left.m03 * right.m30 + left.m13 * right.m31 + left.m23 * right.m32 + left.m33 * right.m33;

        dest.m00 = m00;
        dest.m01 = m01;
        dest.m02 = m02;
        dest.m03 = m03;
        dest.m10 = m10;
        dest.m11 = m11;
        dest.m12 = m12;
        dest.m13 = m13;
        dest.m20 = m20;
        dest.m21 = m21;
        dest.m22 = m22;
        dest.m23 = m23;
        dest.m30 = m30;
        dest.m31 = m31;
        dest.m32 = m32;
        dest.m33 = m33;

        return dest;
    }

    /**
     * Transform a Vector by a matrix and return the result in a destination
     * vector.
     *
     * @param left  The left matrix
     * @param right The right vector
     * @param dest  The destination vector, or null if a new one is to be created
     * @return the destination vector
     */
    public static Vector4f transform(DoubleMatrix left, Vector4f right, Vector4f dest) {
        if (dest == null)
            dest = new Vector4f();

        float x = (float) (left.m00 * right.x() + left.m10 * right.y() + left.m20 * right.z() + left.m30 * right.w());
        float y = (float) (left.m01 * right.x() + left.m11 * right.y() + left.m21 * right.z() + left.m31 * right.w());
        float z = (float) (left.m02 * right.x() + left.m12 * right.y() + left.m22 * right.z() + left.m32 * right.w());
        float w = (float) (left.m03 * right.x() + left.m13 * right.y() + left.m23 * right.z() + left.m33 * right.w());

        dest.set(x, y, z, w);

        return dest;
    }

    /**
     * Scales the source matrix and put the result in the destination matrix
     *
     * @param vec  The vector to scale by
     * @param src  The source matrix
     * @param dest The destination matrix, or null if a new matrix is to be created
     * @return The scaled matrix
     */
    public static DoubleMatrix scale(Vec3 vec, DoubleMatrix src, DoubleMatrix dest) {
        if (dest == null)
            dest = new DoubleMatrix();
        dest.m00 = src.m00 * vec.x();
        dest.m01 = src.m01 * vec.x();
        dest.m02 = src.m02 * vec.x();
        dest.m03 = src.m03 * vec.x();
        dest.m10 = src.m10 * vec.y();
        dest.m11 = src.m11 * vec.y();
        dest.m12 = src.m12 * vec.y();
        dest.m13 = src.m13 * vec.y();
        dest.m20 = src.m20 * vec.z();
        dest.m21 = src.m21 * vec.z();
        dest.m22 = src.m22 * vec.z();
        dest.m23 = src.m23 * vec.z();
        return dest;
    }

    /**
     * Rotates the source matrix around the given axis the specified angle and
     * put the result in the destination matrix.
     *
     * @param angle the angle, in radians.
     * @param axis  The vector representing the rotation axis. Must be normalized.
     * @param src   The matrix to rotate
     * @param dest  The matrix to put the result, or null if a new matrix is to be created
     * @return The rotated matrix
     */
    public static DoubleMatrix rotate(double angle, Vec3 axis, DoubleMatrix src, DoubleMatrix dest) {
        if (dest == null)
            dest = new DoubleMatrix();
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double oneminusc = 1.0f - c;
        double xy = axis.x() * axis.y();
        double yz = axis.y() * axis.z();
        double xz = axis.x() * axis.z();
        double xs = axis.x() * s;
        double ys = axis.y() * s;
        double zs = axis.z() * s;

        double f00 = axis.x() * axis.x() * oneminusc + c;
        double f01 = xy * oneminusc + zs;
        double f02 = xz * oneminusc - ys;
        // n[3] not used
        double f10 = xy * oneminusc - zs;
        double f11 = axis.y() * axis.y() * oneminusc + c;
        double f12 = yz * oneminusc + xs;
        // n[7] not used
        double f20 = xz * oneminusc + ys;
        double f21 = yz * oneminusc - xs;
        double f22 = axis.z() * axis.z() * oneminusc + c;

        double t00 = src.m00 * f00 + src.m10 * f01 + src.m20 * f02;
        double t01 = src.m01 * f00 + src.m11 * f01 + src.m21 * f02;
        double t02 = src.m02 * f00 + src.m12 * f01 + src.m22 * f02;
        double t03 = src.m03 * f00 + src.m13 * f01 + src.m23 * f02;
        double t10 = src.m00 * f10 + src.m10 * f11 + src.m20 * f12;
        double t11 = src.m01 * f10 + src.m11 * f11 + src.m21 * f12;
        double t12 = src.m02 * f10 + src.m12 * f11 + src.m22 * f12;
        double t13 = src.m03 * f10 + src.m13 * f11 + src.m23 * f12;
        dest.m20 = src.m00 * f20 + src.m10 * f21 + src.m20 * f22;
        dest.m21 = src.m01 * f20 + src.m11 * f21 + src.m21 * f22;
        dest.m22 = src.m02 * f20 + src.m12 * f21 + src.m22 * f22;
        dest.m23 = src.m03 * f20 + src.m13 * f21 + src.m23 * f22;
        dest.m00 = t00;
        dest.m01 = t01;
        dest.m02 = t02;
        dest.m03 = t03;
        dest.m10 = t10;
        dest.m11 = t11;
        dest.m12 = t12;
        dest.m13 = t13;
        return dest;
    }

    /**
     * Translate the source matrix and stash the result in the destination matrix
     *
     * @param vec  The vector to translate by
     * @param src  The source matrix
     * @param dest The destination matrix or null if a new matrix is to be created
     * @return The translated matrix
     */
    public static DoubleMatrix translate(Vec3 vec, DoubleMatrix src, DoubleMatrix dest) {
        if (dest == null)
            dest = new DoubleMatrix();

        dest.m30 += src.m00 * vec.x() + src.m10 * vec.y() + src.m20 * vec.z();
        dest.m31 += src.m01 * vec.x() + src.m11 * vec.y() + src.m21 * vec.z();
        dest.m32 += src.m02 * vec.x() + src.m12 * vec.y() + src.m22 * vec.z();
        dest.m33 += src.m03 * vec.x() + src.m13 * vec.y() + src.m23 * vec.z();

        return dest;
    }

    /**
     * Transpose the source matrix and place the result in the destination matrix
     *
     * @param src  The source matrix
     * @param dest The destination matrix or null if a new matrix is to be created
     * @return the transposed matrix
     */
    public static DoubleMatrix transpose(DoubleMatrix src, DoubleMatrix dest) {
        if (dest == null)
            dest = new DoubleMatrix();
        double m00 = src.m00;
        double m01 = src.m10;
        double m02 = src.m20;
        double m03 = src.m30;
        double m10 = src.m01;
        double m11 = src.m11;
        double m12 = src.m21;
        double m13 = src.m31;
        double m20 = src.m02;
        double m21 = src.m12;
        double m22 = src.m22;
        double m23 = src.m32;
        double m30 = src.m03;
        double m31 = src.m13;
        double m32 = src.m23;
        double m33 = src.m33;

        dest.m00 = m00;
        dest.m01 = m01;
        dest.m02 = m02;
        dest.m03 = m03;
        dest.m10 = m10;
        dest.m11 = m11;
        dest.m12 = m12;
        dest.m13 = m13;
        dest.m20 = m20;
        dest.m21 = m21;
        dest.m22 = m22;
        dest.m23 = m23;
        dest.m30 = m30;
        dest.m31 = m31;
        dest.m32 = m32;
        dest.m33 = m33;

        return dest;
    }

    /**
     * Calculate the determinant of a 3x3 matrix
     *
     * @return result
     */

    private static double determinant3x3(double t00, double t01, double t02,
                                         double t10, double t11, double t12,
                                         double t20, double t21, double t22) {
        return t00 * (t11 * t22 - t12 * t21)
                + t01 * (t12 * t20 - t10 * t22)
                + t02 * (t10 * t21 - t11 * t20);
    }

    /**
     * Invert the source matrix and put the result in the destination
     *
     * @param src  The source matrix
     * @param dest The destination matrix, or null if a new matrix is to be created
     * @return The inverted matrix if successful, null otherwise
     */
    public static DoubleMatrix invert(DoubleMatrix src, DoubleMatrix dest) {
        double determinant = src.determinant();

        if (determinant != 0) {
            /*
             * m00 m01 m02 m03
             * m10 m11 m12 m13
             * m20 m21 m22 m23
             * m30 m31 m32 m33
             */
            if (dest == null)
                dest = new DoubleMatrix();
            double determinant_inv = 1f / determinant;

            // first row
            double t00 = determinant3x3(src.m11, src.m12, src.m13, src.m21, src.m22, src.m23, src.m31, src.m32, src.m33);
            double t01 = -determinant3x3(src.m10, src.m12, src.m13, src.m20, src.m22, src.m23, src.m30, src.m32, src.m33);
            double t02 = determinant3x3(src.m10, src.m11, src.m13, src.m20, src.m21, src.m23, src.m30, src.m31, src.m33);
            double t03 = -determinant3x3(src.m10, src.m11, src.m12, src.m20, src.m21, src.m22, src.m30, src.m31, src.m32);
            // second row
            double t10 = -determinant3x3(src.m01, src.m02, src.m03, src.m21, src.m22, src.m23, src.m31, src.m32, src.m33);
            double t11 = determinant3x3(src.m00, src.m02, src.m03, src.m20, src.m22, src.m23, src.m30, src.m32, src.m33);
            double t12 = -determinant3x3(src.m00, src.m01, src.m03, src.m20, src.m21, src.m23, src.m30, src.m31, src.m33);
            double t13 = determinant3x3(src.m00, src.m01, src.m02, src.m20, src.m21, src.m22, src.m30, src.m31, src.m32);
            // third row
            double t20 = determinant3x3(src.m01, src.m02, src.m03, src.m11, src.m12, src.m13, src.m31, src.m32, src.m33);
            double t21 = -determinant3x3(src.m00, src.m02, src.m03, src.m10, src.m12, src.m13, src.m30, src.m32, src.m33);
            double t22 = determinant3x3(src.m00, src.m01, src.m03, src.m10, src.m11, src.m13, src.m30, src.m31, src.m33);
            double t23 = -determinant3x3(src.m00, src.m01, src.m02, src.m10, src.m11, src.m12, src.m30, src.m31, src.m32);
            // fourth row
            double t30 = -determinant3x3(src.m01, src.m02, src.m03, src.m11, src.m12, src.m13, src.m21, src.m22, src.m23);
            double t31 = determinant3x3(src.m00, src.m02, src.m03, src.m10, src.m12, src.m13, src.m20, src.m22, src.m23);
            double t32 = -determinant3x3(src.m00, src.m01, src.m03, src.m10, src.m11, src.m13, src.m20, src.m21, src.m23);
            double t33 = determinant3x3(src.m00, src.m01, src.m02, src.m10, src.m11, src.m12, src.m20, src.m21, src.m22);

            // transpose and divide by the determinant
            dest.m00 = t00 * determinant_inv;
            dest.m11 = t11 * determinant_inv;
            dest.m22 = t22 * determinant_inv;
            dest.m33 = t33 * determinant_inv;
            dest.m01 = t10 * determinant_inv;
            dest.m10 = t01 * determinant_inv;
            dest.m20 = t02 * determinant_inv;
            dest.m02 = t20 * determinant_inv;
            dest.m12 = t21 * determinant_inv;
            dest.m21 = t12 * determinant_inv;
            dest.m03 = t30 * determinant_inv;
            dest.m30 = t03 * determinant_inv;
            dest.m13 = t31 * determinant_inv;
            dest.m31 = t13 * determinant_inv;
            dest.m32 = t23 * determinant_inv;
            dest.m23 = t32 * determinant_inv;
            return dest;
        } else
            return null;
    }

    /**
     * Negate this matrix and place the result in a destination matrix.
     *
     * @param src  The source matrix
     * @param dest The destination matrix, or null if a new matrix is to be created
     * @return The negated matrix
     */
    public static DoubleMatrix negate(DoubleMatrix src, DoubleMatrix dest) {
        if (dest == null)
            dest = new DoubleMatrix();

        dest.m00 = -src.m00;
        dest.m01 = -src.m01;
        dest.m02 = -src.m02;
        dest.m03 = -src.m03;
        dest.m10 = -src.m10;
        dest.m11 = -src.m11;
        dest.m12 = -src.m12;
        dest.m13 = -src.m13;
        dest.m20 = -src.m20;
        dest.m21 = -src.m21;
        dest.m22 = -src.m22;
        dest.m23 = -src.m23;
        dest.m30 = -src.m30;
        dest.m31 = -src.m31;
        dest.m32 = -src.m32;
        dest.m33 = -src.m33;

        return dest;
    }

    /**
     * Returns a string representation of this matrix
     */
    @Override
    public String toString() {
        String buf = String.valueOf(m00) + ' ' + m10 + ' ' + m20 + ' ' + m30 + '\n' +
                m01 + ' ' + m11 + ' ' + m21 + ' ' + m31 + '\n' +
                m02 + ' ' + m12 + ' ' + m22 + ' ' + m32 + '\n' +
                m03 + ' ' + m13 + ' ' + m23 + ' ' + m33 + '\n';
        return buf;
    }

    /**
     * Set this matrix to be the identity matrix.
     *
     * @return this
     */
    public DoubleMatrix setIdentity() {
        return setIdentity(this);
    }

    /**
     * Set this matrix to 0.
     *
     * @return this
     */
    public DoubleMatrix setZero() {
        return setZero(this);
    }

    /**
     * Load from another matrix4f
     *
     * @param src The source matrix
     * @return this
     */
    public DoubleMatrix load(DoubleMatrix src) {
        return load(src, this);
    }

    /**
     * Load from a double buffer. The buffer stores the matrix in column major
     * (OpenGL) order.
     *
     * @param buf A double buffer to read from
     * @return this
     */
    public DoubleMatrix load(DoubleBuffer buf) {

        m00 = buf.get();
        m01 = buf.get();
        m02 = buf.get();
        m03 = buf.get();
        m10 = buf.get();
        m11 = buf.get();
        m12 = buf.get();
        m13 = buf.get();
        m20 = buf.get();
        m21 = buf.get();
        m22 = buf.get();
        m23 = buf.get();
        m30 = buf.get();
        m31 = buf.get();
        m32 = buf.get();
        m33 = buf.get();

        return this;
    }

    /**
     * Load from a double buffer. The buffer stores the matrix in row major
     * (maths) order.
     *
     * @param buf A double buffer to read from
     * @return this
     */
    public DoubleMatrix loadTranspose(DoubleBuffer buf) {

        m00 = buf.get();
        m10 = buf.get();
        m20 = buf.get();
        m30 = buf.get();
        m01 = buf.get();
        m11 = buf.get();
        m21 = buf.get();
        m31 = buf.get();
        m02 = buf.get();
        m12 = buf.get();
        m22 = buf.get();
        m32 = buf.get();
        m03 = buf.get();
        m13 = buf.get();
        m23 = buf.get();
        m33 = buf.get();

        return this;
    }

    /**
     * Store this matrix in a double buffer. The matrix is stored in column
     * major (openGL) order.
     *
     * @param buf The buffer to store this matrix in
     */
    public DoubleMatrix store(DoubleBuffer buf) {
        buf.put(m00);
        buf.put(m01);
        buf.put(m02);
        buf.put(m03);
        buf.put(m10);
        buf.put(m11);
        buf.put(m12);
        buf.put(m13);
        buf.put(m20);
        buf.put(m21);
        buf.put(m22);
        buf.put(m23);
        buf.put(m30);
        buf.put(m31);
        buf.put(m32);
        buf.put(m33);
        return this;
    }

    /**
     * Store this matrix in a double buffer. The matrix is stored in row
     * major (maths) order.
     *
     * @param buf The buffer to store this matrix in
     */
    public DoubleMatrix storeTranspose(DoubleBuffer buf) {
        buf.put(m00);
        buf.put(m10);
        buf.put(m20);
        buf.put(m30);
        buf.put(m01);
        buf.put(m11);
        buf.put(m21);
        buf.put(m31);
        buf.put(m02);
        buf.put(m12);
        buf.put(m22);
        buf.put(m32);
        buf.put(m03);
        buf.put(m13);
        buf.put(m23);
        buf.put(m33);
        return this;
    }

    /**
     * Store the rotation portion of this matrix in a double buffer. The matrix is stored in column
     * major (openGL) order.
     *
     * @param buf The buffer to store this matrix in
     */
    public DoubleMatrix store3f(DoubleBuffer buf) {
        buf.put(m00);
        buf.put(m01);
        buf.put(m02);
        buf.put(m10);
        buf.put(m11);
        buf.put(m12);
        buf.put(m20);
        buf.put(m21);
        buf.put(m22);
        return this;
    }

    /**
     * Transpose this matrix
     *
     * @return this
     */
    public DoubleMatrix transpose() {
        return this.transpose(this);
    }

    /**
     * Translate this matrix
     *
     * @param vec The vector to translate by
     * @return this
     */
    public DoubleMatrix translate(Vec3 vec) {
        return this.translate(vec, this);
    }

    /**
     * Scales this matrix
     *
     * @param vec The vector to scale by
     * @return this
     */
    public DoubleMatrix scale(Vec3 vec) {
        return scale(vec, this, this);
    }

    /**
     * Rotates the matrix around the given axis the specified angle
     *
     * @param angle the angle, in radians.
     * @param axis  The vector representing the rotation axis. Must be normalized.
     * @return this
     */
    public DoubleMatrix rotate(double angle, Vec3 axis) {
        return this.rotate(angle, axis, this);
    }

    /**
     * Rotates the matrix around the given axis the specified angle
     *
     * @param angle the angle, in radians.
     * @param axis  The vector representing the rotation axis. Must be normalized.
     * @param dest  The matrix to put the result, or null if a new matrix is to be created
     * @return The rotated matrix
     */
    public DoubleMatrix rotate(double angle, Vec3 axis, DoubleMatrix dest) {
        return rotate(angle, axis, this, dest);
    }

    /**
     * Translate this matrix and stash the result in another matrix
     *
     * @param vec  The vector to translate by
     * @param dest The destination matrix or null if a new matrix is to be created
     * @return the translated matrix
     */
    public DoubleMatrix translate(Vec3 vec, DoubleMatrix dest) {
        return translate(vec, this, dest);
    }

    /**
     * Transpose this matrix and place the result in another matrix
     *
     * @param dest The destination matrix or null if a new matrix is to be created
     * @return the transposed matrix
     */
    public DoubleMatrix transpose(DoubleMatrix dest) {
        return transpose(this, dest);
    }

    /**
     * @return the determinant of the matrix
     */
    public double determinant() {
        double f =
                m00
                        * ((m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32)
                        - m13 * m22 * m31
                        - m11 * m23 * m32
                        - m12 * m21 * m33);
        f -= m01
                * ((m10 * m22 * m33 + m12 * m23 * m30 + m13 * m20 * m32)
                - m13 * m22 * m30
                - m10 * m23 * m32
                - m12 * m20 * m33);
        f += m02
                * ((m10 * m21 * m33 + m11 * m23 * m30 + m13 * m20 * m31)
                - m13 * m21 * m30
                - m10 * m23 * m31
                - m11 * m20 * m33);
        f -= m03
                * ((m10 * m21 * m32 + m11 * m22 * m30 + m12 * m20 * m31)
                - m12 * m21 * m30
                - m10 * m22 * m31
                - m11 * m20 * m32);
        return f;
    }

    /**
     * Invert this matrix
     *
     * @return this if successful, null otherwise
     */
    public DoubleMatrix invert() {
        return invert(this, this);
    }

    /**
     * Negate this matrix
     *
     * @return this
     */
    public DoubleMatrix negate() {
        return this.negate(this);
    }

    /**
     * Negate this matrix and place the result in a destination matrix.
     *
     * @param dest The destination matrix, or null if a new matrix is to be created
     * @return the negated matrix
     */
    public DoubleMatrix negate(DoubleMatrix dest) {
        return negate(this, dest);
    }
}

