package spring.application.tree.data.utility.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrioValue<A, B, C> {
    private A key;
    private B value;
    private C data;
}
