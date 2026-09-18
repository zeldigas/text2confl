require 'asciidoctor'
require 'asciidoctor/extensions'

class UserMacro < Asciidoctor::Extensions::InlineMacroProcessor
  use_dsl

  named :user

  # @param target [String]
  def process parent, target, attrs
    resolver = parent.document.attr 't2c-user-resolver'
    resolved_user = resolver.resolve target
    if resolved_user.nil?
      create_inline parent, :quoted, escape_autolink(target)
    else
      value = escape_autolink(resolved_user['value'])
      user_link = %(<ac:link><ri:user #{resolved_user['attr']}="#{value}" /></ac:link>)
      create_inline parent, :quoted, user_link
    end
  end

  private

  # A bare email is auto-linked by Asciidoctor's own substitutions when it
  # rescans this macro's output, corrupting ri:username/plain text. A leading
  # backslash is Asciidoctor's escape convention: it suppresses the autolink
  # and is itself stripped, leaving plain text. See commit a02cf03 (#135).
  def escape_autolink value
    Asciidoctor::InlineEmailRx.match(value) ? %(\\#{value}) : value
  end
end
