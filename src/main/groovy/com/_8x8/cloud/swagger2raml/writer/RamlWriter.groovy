package com._8x8.cloud.swagger2raml.writer

import com._8x8.cloud.swagger2raml.model.Method
import com._8x8.cloud.swagger2raml.model.Path
import com._8x8.cloud.swagger2raml.model.QueryParameter
import com._8x8.cloud.swagger2raml.model.Resource
import com._8x8.cloud.swagger2raml.model.SchemaProperty
import groovy.json.JsonBuilder
import groovy.transform.TypeChecked

/**
 * @author Jacek Kunicki
 */
@TypeChecked
class RamlWriter {

    private static final String LINE_SEPARATOR = System.getProperty('line.separator')
    private static final String RAML_HEADER = '#%RAML 0.8'
    private static final int TAB_WIDTH = 2

    private int indentation = 0

    private File file
    private StringBuilder stringBuilder = new StringBuilder()

    void save() {
        file << stringBuilder.toString()
    }

    void writeHeader() {
        write(RAML_HEADER)
        write('---')
    }

    void writeResources(Collection<Resource> resources) {
        resources.each { resource ->
            write("${Path.SEPARATOR}${resource.path}:")
            indented {
                resource.methods.each { writeMethod(it) }
                writeResources(resource.children)
            }
        }
    }

    private void writeMethod(Method method) {
        write("${method.class.simpleName.toLowerCase()}:")

        indented {
            if (method.description) {
                writeProperty(method, 'description')
            }

            if (method.queryParameters) {
                write('queryParameters:')
                indented {
                    method.queryParameters.each { writeQueryParameter(it) }
                }
            }

            if (method.body) {
                write('body:')
                indented {
                    write("${method.body.contentType}:")
                    indented {
                        write('schema: |')
                        def schema = [
                                type      : method.body.schema.type,
                                properties: method.body.schema.properties.collectEntries { SchemaProperty p -> p.extractSchema() }
                        ]

                        indented {
                            write(new JsonBuilder(schema).toPrettyString())
                        }

                        if (method.body.example) {
                            write('example: |')
                            indented {
                                write(method.body.example)
                            }
                        }
                    }
                }
            }

            if (method.responses) {
                write('responses:')
                indented {
                    write('200:')
                    indented {
                        write('body:')
                        indented {
                            method.responses.each { response ->
                                write("${response.contentType}:")
                                indented {
                                    write('schema: {}')
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void writeQueryParameter(QueryParameter queryParameter) {
        write("${queryParameter.name}:")
        indented {
            writeProperties(queryParameter, ['displayName', 'type', 'description', 'example', 'required', 'repeat'])
        }
    }

    void writeProperties(object, Collection<String> propertyNames) {
        propertyNames.each { writeProperty(object, it) }
    }

    private void writeProperty(object, String propertyName) {
        // explicit null check (instead of Groovy truth) since a false value is valid
        if (object[propertyName] != null) {
            write("${propertyName}: ${object[propertyName]}")
        }
    }

    private void write(String s) {
        s.split('[\r\n]+').each { String line ->
            stringBuilder << ' ' * indentation << line << LINE_SEPARATOR
        }
    }

    private void indent() {
        indentation += TAB_WIDTH
    }

    private void unident() {
        indentation -= TAB_WIDTH
    }

    private void indented(Closure closure) {
        indent()
        closure.call()
        unident()
    }
}
