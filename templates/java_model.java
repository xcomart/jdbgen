package com.abc.sample.${name.suffix.camel};

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Data;
import org.apache.ibatis.type.Alias;

/**
 * ${remarks} model class
 * 
 * @ClassName ${name.prefix.pascal}.java
 * @Description ${remarks} model class
 * @author ${user}
 * @since ${date:YYYY. M. d.}
 *
 */
@Data
@Alias("${name.suffix.lower}")
public class ${name.suffix.pascal}Model
{
    ${for:item=keys}// ${remarks}
    @NotBlank(message="${remarks}: Required Item.")
    private ${item:key=javaType,padSize=10,padDir=right} ${name.camel};
    ${endfor}

    ${for:item=notKeys}// ${remarks}
    ${if:key=nullable,value=0}@NotBlank(message="${remarks}: Required Item.")
    ${endif}${if:key=typeName,startsWith="char"}@Size(max=${length}, message="${remarks}: Cannot exceeds ${length}.")
    ${endif}private ${item:key=javaType,padSize=10,padDir=right} ${name.camel};
    ${endfor}
}
