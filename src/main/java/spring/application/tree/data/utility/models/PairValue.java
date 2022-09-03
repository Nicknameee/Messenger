package spring.application.tree.data.utility.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PairValue<A, B> {
    private A key;
    private B value;
}
