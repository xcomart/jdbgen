package com.abc.sample.${name.prefix.camel}.model;

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
@Alias("${name.prefix.lower}")
public class ${name.prefix.pascal}Model
{
    ${for:item=keys,inStr="\n"}
    // ${remarks}
    @NotBlank(message="${remarks}: Required Item.")
    private ${item:key=javaType,padSize=10,padDir=right} ${name.camel};
    ${endfor}

    ${for:item=notKeys,inStr="\n",skipList="last_updater_id,last_updated_date"}
    // ${remarks}
    ${if:key=nullable,value=0}@NotBlank(message="${remarks}: Required Item.", groups=PostMethod.class)
    ${elif:key=typeName,value="varchar"}@Size(max=${length}, message="${remarks}: Cannot exceeds ${length}.", groups=PostMethod.class)
    ${endif}private ${item:key=javaType,padSize=10,padDir=right} ${name.camel};

    ${endfor}
}
