require 'asciidoctor'
require 'asciidoctor/extensions'

class StatusMacro < Asciidoctor::Extensions::InlineMacroProcessor
  use_dsl

  named :status
  name_positional_attributes 'value'

  # @param target [String]
  def process parent, target, attrs
    status = %(<ac:structured-macro ac:name="status"><ac:parameter ac:name="colour">#{target.capitalize}</ac:parameter>)
    if (text = attrs['value'])
      status += %(<ac:parameter ac:name="title">#{text}</ac:parameter>)
    end
    status += %(</ac:structured-macro>)
    create_inline parent, :quoted, status
  end
end