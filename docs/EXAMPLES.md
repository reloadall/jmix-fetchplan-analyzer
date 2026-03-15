# Examples

## Example 1 — direct path usage

### Code
```java
public void go(Contract contract) {
    System.out.println(contract.getType().getCode());
}
```

Canonical paths
`type.code`

## Example 2 — collections
### Code
```java
public void go(Contract contract) {
    for (Subcontract subcontract : contract.getSubcontracts()) {
        System.out.println(subcontract.getStatus());
    }
}
```

Canonical paths
`subcontracts.status`

## Example 3 — interproc
### Code
```java
public void go(Contract contract) {
    someMethod(contract.getEmployee());
}

private void someMethod(Employee employee) {
    System.out.println(employee.getDepartment().getName());
}
```

Canonical paths
`employee.department.name`

## Example 4 — value-call interproc
### Code
```java
public void go(Contract contract) {
    Department x = (Department) someMethod(contract.getEmployee());
    System.out.println(x.getName());
}

private Object someMethod(Employee employee) {
    return employee.getDepartment();
}
```

Canonical paths
`employee.department.name`

## Example 5 — uncertainty
### Code
```java
public void go(Contract contract) {
    Object x = someUnknownMethod(contract.getEmployee());
    System.out.println(x);
}
```

Expected result
uncertainty is reported for the affected path zone
