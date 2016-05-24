package com.example.edwinkurniawan.ar;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Edwin Kurniawan on 5/4/2016.
 */
public class Triangle {

    private FloatBuffer vertexBuffer;
    private float base = 1.0f;
    private float red, green, blue;
    private float vertices[] = {
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
            0.0f, 0.5f, 0.0f
    };

    public Triangle(float scale, float red, float green, float blue) {
        vertices = new float[] {
                -base * scale, -base * scale, 0.0f,
                base * scale, -base * scale, 0.0f,
                0.0f, base * scale, 0.0f
        };
        this.red = red;
        this.green = green;
        this.blue = blue;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(3*3*4);
        byteBuffer.order(ByteOrder.nativeOrder());
        vertexBuffer = byteBuffer.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.flip();
    }

    public void draw(GL10 gl) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glColor4f(red,green,blue,0.5f);
        gl.glVertexPointer(3,GL10.GL_FLOAT,0,vertexBuffer);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, vertices.length / 3);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

}
