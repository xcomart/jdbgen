
package comart.utils.tuple;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Pair<T, U> {
    private T first;
    private U second;
}
