#version 330 core
// Original standalone effect: silhouette fill, animated colors and bounded edge sampling.
uniform sampler2D u_Texture;
uniform vec4 u_Fill;
uniform vec4 u_Outline;
uniform float u_Opacity;
uniform float u_Time;
uniform float u_Speed;
uniform int u_FillMode;
uniform int u_Width;
uniform int u_Spacing;
uniform bool u_Glow;
uniform bool u_Dots;
in vec2 v_TexCoord;
in vec2 v_OneTexel;
out vec4 color;
vec3 rainbow(float t) {
    return 0.55+0.45*cos(6.2831853*(vec3(0.0,0.3333333,0.6666667)+t));
}
void main() {
    float mask=texture(u_Texture,v_TexCoord).a;
    vec3 fill=u_Fill.rgb;
    float phase=u_Time*u_Speed*0.12;
    if(u_FillMode==1)fill*=0.55+0.45*sin(v_TexCoord.y*10.0+phase*6.2831853);
    if(u_FillMode==2)fill=rainbow(phase+v_TexCoord.y*0.4);
    float opacity=mask*u_Opacity*u_Fill.a;
    if(u_Dots) {
        vec2 pixel=v_TexCoord/v_OneTexel;
        vec2 cell=mod(pixel,float(u_Spacing))-float(u_Spacing)*0.5;
        opacity*=mix(0.18,1.0,1.0-smoothstep(1.2,2.2,length(cell)));
    }
    color=vec4(fill,opacity);
    if(u_Width>0&&mask<0.05) {
        float edge=0.0,halo=0.0;
        for(int i=1;i<=6;i++) {
            if(i>u_Width)break;
            for(int j=0;j<12;j++) {
                float a=float(j)*0.5235987756;
                vec2 direction=vec2(cos(a),sin(a));
                edge=max(edge,texture(u_Texture,v_TexCoord+direction*v_OneTexel*float(i)).a);
                if(u_Glow)halo=max(halo,texture(u_Texture,v_TexCoord+direction*v_OneTexel*float(i*2)).a*(1.0-float(i)/7.0));
            }
        }
        color=vec4(u_Outline.rgb,max(edge,halo*0.45)*u_Outline.a);
    }
    if(color.a<0.001)discard;
}
