package io.github.reloadall.fetchplan.analyzer.jmix.interproc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeclaredTypeDescriptorTest {

    @Test
    void parsesListElementType() {
        DeclaredTypeDescriptor descriptor = DeclaredTypeDescriptor.parse("List<DocumentWorker>");

        assertEquals("List<DocumentWorker>", descriptor.getRawDeclaredTypeName());
        assertEquals("List", descriptor.getRawTypeName());
        assertTrue(descriptor.isSupportedCollectionContainer());
        assertEquals("DocumentWorker", descriptor.getCollectionElementTypeName().orElseThrow());
    }

    @Test
    void parsesCollectionElementType() {
        DeclaredTypeDescriptor descriptor = DeclaredTypeDescriptor.parse("Collection<DocumentWorker>");

        assertTrue(descriptor.isSupportedCollectionContainer());
        assertEquals("DocumentWorker", descriptor.getCollectionElementTypeName().orElseThrow());
    }

    @Test
    void parsesIterableElementType() {
        DeclaredTypeDescriptor descriptor = DeclaredTypeDescriptor.parse("Iterable<DocumentWorker>");

        assertTrue(descriptor.isSupportedCollectionContainer());
        assertEquals("DocumentWorker", descriptor.getCollectionElementTypeName().orElseThrow());
    }

    @Test
    void returnsEmptyElementTypeForRawList() {
        DeclaredTypeDescriptor descriptor = DeclaredTypeDescriptor.parse("List");

        assertTrue(descriptor.isSupportedCollectionContainer());
        assertTrue(descriptor.getCollectionElementTypeName().isEmpty());
    }

    @Test
    void returnsEmptyElementTypeForUnsupportedGenericShape() {
        DeclaredTypeDescriptor descriptor = DeclaredTypeDescriptor.parse("List<Map<String, DocumentWorker>>");

        assertTrue(descriptor.isSupportedCollectionContainer());
        assertTrue(descriptor.getCollectionElementTypeName().isEmpty());
    }

    @Test
    void marksNonCollectionTypeAsNotSupportedCollectionContainer() {
        DeclaredTypeDescriptor descriptor = DeclaredTypeDescriptor.parse("DocumentWorker");

        assertFalse(descriptor.isSupportedCollectionContainer());
        assertTrue(descriptor.getCollectionElementTypeName().isEmpty());
    }
}