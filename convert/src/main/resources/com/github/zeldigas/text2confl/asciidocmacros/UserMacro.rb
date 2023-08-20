require 'asciidoctor'
require 'asciidoctor/extensions'

class UserMacro < Asciidoctor::Extensions::InlineMacroProcessor
  use_dsl

  named :user

  # @param target [String]
  def process parent, target, attrs
    create_inline_pass parent, %(<ac:link><ri:user ri:username="#{target}" /></ac:link>)
  end
end