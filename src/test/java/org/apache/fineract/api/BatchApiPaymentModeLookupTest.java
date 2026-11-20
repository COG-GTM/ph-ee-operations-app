package org.apache.fineract.api;

import org.apache.fineract.operations.Transfer;
import org.apache.fineract.operations.Variable;
import org.apache.fineract.operations.VariableRepository;
import org.junit.Test;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BatchApiPaymentModeLookupTest {

    private final List<List<Long>> queriedChunks = new ArrayList<>();

    private BatchApi batchApi(List<Variable> stored) throws Exception {
        VariableRepository repository = (VariableRepository) Proxy.newProxyInstance(
                VariableRepository.class.getClassLoader(), new Class<?>[]{VariableRepository.class},
                (proxy, method, args) -> {
                    if (!method.getName().equals("findAllByVariableNameAndWorkflowInstanceKeyIn")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    Collection<?> keys = (Collection<?>) args[1];
                    List<Long> chunk = new ArrayList<>();
                    keys.forEach(k -> chunk.add((Long) k));
                    queriedChunks.add(chunk);
                    List<Variable> result = new ArrayList<>();
                    for (Variable v : stored) {
                        if (v.getName().equals(args[0]) && keys.contains(v.getWorkflowInstanceKey())) {
                            result.add(v);
                        }
                    }
                    return result;
                });
        BatchApi api = new BatchApi();
        Field field = BatchApi.class.getDeclaredField("variableRepository");
        field.setAccessible(true);
        field.set(api, repository);
        return api;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Variable> lookup(BatchApi api, List<Transfer> transfers) throws Exception {
        Method method = BatchApi.class.getDeclaredMethod("findPaymentModeVariables", List.class);
        method.setAccessible(true);
        try {
            return (Map<Long, Variable>) method.invoke(api, transfers);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    private static Transfer transfer(Long key) {
        Transfer t = new Transfer();
        t.setWorkflowInstanceKey(key);
        return t;
    }

    private static Variable variable(String name, long key) {
        Variable v = new Variable();
        v.setName(name);
        v.setWorkflowInstanceKey(key);
        return v;
    }

    @Test
    public void mapsEachTransferToItsPaymentModeAcrossChunks() throws Exception {
        List<Variable> stored = new ArrayList<>();
        List<Transfer> transfers = new ArrayList<>();
        for (long k = 1; k <= 2500; k++) {
            transfers.add(transfer(k));
            if (k % 3 != 0) {
                stored.add(variable("paymentMode", k));
            }
            stored.add(variable("amount", k));
        }
        transfers.add(transfer(7L));
        transfers.add(transfer(null));

        Map<Long, Variable> modes = lookup(batchApi(stored), transfers);

        assertEquals(3, queriedChunks.size());
        assertEquals(1000, queriedChunks.get(0).size());
        assertEquals(500, queriedChunks.get(2).size());
        assertFalse(queriedChunks.stream().anyMatch(c -> c.contains(null)));
        assertEquals(2500 - 833, modes.size());
        assertEquals("paymentMode", modes.get(1L).getName());
        assertNull(modes.get(3L));
        assertNull(modes.get(null));
    }

    @Test
    public void emptyBatchRunsNoQuery() throws Exception {
        assertTrue(lookup(batchApi(new ArrayList<>()), new ArrayList<>()).isEmpty());
        assertTrue(queriedChunks.isEmpty());
    }

    @Test
    public void duplicatePaymentModeFailsLikeTheSingleRowLookup() throws Exception {
        List<Variable> stored = new ArrayList<>();
        stored.add(variable("paymentMode", 5L));
        stored.add(variable("paymentMode", 5L));
        List<Transfer> transfers = new ArrayList<>();
        transfers.add(transfer(5L));
        try {
            lookup(batchApi(stored), transfers);
            fail("expected IncorrectResultSizeDataAccessException");
        } catch (IncorrectResultSizeDataAccessException expected) {
            assertEquals(1, expected.getExpectedSize());
        }
    }
}
