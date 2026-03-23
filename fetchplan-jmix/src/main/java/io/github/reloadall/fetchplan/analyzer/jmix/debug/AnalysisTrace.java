package io.github.reloadall.fetchplan.analyzer.jmix.debug;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.springframework.stereotype.Component;

@Component("fpa_AnalysisTrace")
public class AnalysisTrace {

    private final ThreadLocal<List<String>> events =
            ThreadLocal.withInitial(ArrayList::new);

    private final ThreadLocal<Set<String>> enteredMethods =
            ThreadLocal.withInitial(LinkedHashSet::new);

    public void start(String title) {
        clear();
        log("START: " + title);
    }

    public void log(String message) {
        events.get().add(message);
    }

    public void logMethodEntry(MethodDeclaration method) {
        String key = methodKey(method);
        if (enteredMethods.get().add(key)) {
            log("ENTER: " + key);
        }
    }

    public String dump() {
        List<String> values = events.get();
        if (values == null || values.isEmpty()) {
            return "<empty trace>";
        }
        return String.join("\n", values);
    }

    public void clear() {
        events.remove();
        enteredMethods.remove();
    }

    private String methodKey(MethodDeclaration method) {
        String owner = method.findAncestor(TypeDeclaration.class)
                .map(TypeDeclaration::getNameAsString)
                .orElse("<unknown-type>");

        String params = method.getParameters().stream()
                .map(parameter -> parameter.getType().asString())
                .collect(Collectors.joining(", "));

        return owner + "." + method.getNameAsString() + "(" + params + ")";
    }
}
