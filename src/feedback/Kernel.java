/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package feedback;

/**
 *
 * @author Debasis
 */
public interface Kernel {
    float fKernel(float dist);
}


class GaussianKernel implements Kernel {
    float sigma;
    float h;
    
    public GaussianKernel(float sigma, float h) {
        this.sigma = sigma;
        this.h = h;
    }
    
    @Override
    public float fKernel(float dist) { // scaled kernel K_h(x) = 1/h K(x/h)
        float f, norm;
        dist = 1/h * dist;
        norm = (float)(Math.sqrt(2*Math.PI) * sigma);
        f = (float)Math.exp(-(dist*dist)/(2*sigma*sigma));
        return 1/h * f/norm;
    }    
}

class TriangularKernel implements Kernel {
    float h;
    
    public TriangularKernel(float h) {
        this.h = h;
    }
    
    @Override
    public float fKernel(float dist) {
        return 1/h * (1 - Math.abs(dist/h));
    }    
}
