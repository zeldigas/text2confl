require 'asciidoctor'
require 'asciidoctor/extensions'

class SimpleInlineMacro < Asciidoctor::Extensions::InlineMacroProcessor
  use_dsl

  named :confl_macro

  def process parent, target, attrs
    key = target
    simple_macro = %(<ac:structured-macro ac:name="#{key}">)
    attrs.each do |k, v|
      simple_macro += %(<ac:parameter ac:name="#{k}">#{v}</ac:parameter>)
    end
    simple_macro += %(</ac:structured-macro>)
    create_inline parent, :quoted, simple_macro
  end
end