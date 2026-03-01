require 'asciidoctor'
require 'json'

if Gem::Version.new(Asciidoctor::VERSION) <= Gem::Version.new('2.0.0')
  fail 'asciidoctor: FAILED: HTML5/Slim backend needs Asciidoctor >=1.0.0!'
end

unless defined? Slim::Include
  fail 'asciidoctor: FAILED: HTML5/Slim backend needs Slim >= 4.0.0!'
end

# Add custom functions to this module that you want to use in your Slim
# templates. Within the template you can invoke them as top-level functions
# just like in Haml.
module Slim::Helpers
  QUOTE_TAGS = Asciidoctor::Converter::Html5Converter::QUOTE_TAGS
  # Defaults
  DEFAULT_SECTNUMLEVELS = 3

  VOID_ELEMENTS = %w(area base br col command embed hr img input keygen link meta param source track wbr)

  CG_ALPHA = '[a-zA-Z]'
  CC_ALNUM = 'a-zA-Z0-9'

  # Detects strings that resemble URIs.
  #
  # Examples
  #   http://domain
  #   https://domain
  #   file:///path
  #   data:info
  #
  #   not c:/sample.adoc or c:\sample.adoc
  #
  UriSniffRx = %r{^#{CG_ALPHA}[#{CC_ALNUM}.+-]+:/{0,2}}

  ##
  # Creates an HTML tag with the given name and optionally attributes. Can take
  # a block that will run between the opening and closing tags.
  #
  # @param name [#to_s] the name of the tag.
  # @param attributes [Hash]
  # @param content [#to_s] the content; +nil+ to call the block.
  # @yield The block of Slim/HTML code within the tag (optional).
  # @return [String] a rendered HTML element.
  #
  def html_tag(name, attributes = {}, content = nil)
    attrs = attributes.reject { |_, v|
      v.nil? || (v.respond_to?(:empty?) && v.empty?)
    }.map do |k, v|
      v = v.compact.join(' ') if v.is_a? Array
      v = nil if v == true
      v = %("#{v}") if v
      [k, v] * '='
    end
    attrs_str = attrs.empty? ? '' : attrs.join(' ').prepend(' ')

    if VOID_ELEMENTS.include? name.to_s
      %(<#{name}#{attrs_str}>)
    else
      content ||= yield if block_given?
      %(<#{name}#{attrs_str}>#{content}</#{name}>)
    end
  end

  ##
  # Conditionally wraps a block in an a element. If condition is +true+ then it
  # renders the a tag and the given block inside, otherwise it just renders the block.
  #
  # For example:
  #
  #    = html_a_tag_if link?
  #      img src='./img/tux.png'
  #
  # will produce:
  #
  #    <a href="http://example.org" class="image">
  #      <img src="./img/tux.png">
  #    </a>
  #
  # if +link?+ is truthy, and just
  #
  #   <img src="./img/tux.png">
  #
  # otherwise.
  #
  # @param condition [Boolean] the condition to test to determine whether to
  #        render the enclosing a tag.
  # @yield (see #html_tag)
  # @return [String] a rendered HTML fragment.
  #
  def html_a_tag_if(condition, &block)
    if condition
      html_tag :a, {href: (attr :link)}, &block
    else
      yield
    end
  end

  ##
  # Returns corrected section level.
  #
  # @param sec [Asciidoctor::Section] the section node (default: self).
  # @return [Integer]
  #
  def section_level(sec = self)
    @_section_level ||= (sec.level == 0 && sec.special) ? 1 : sec.level
  end

  ##
  # Returns the captioned section's title, optionally numbered.
  #
  # @param sec [Asciidoctor::Section] the section node (default: self).
  # @return [String]
  #
  def section_title(sec = self)
    sectnumlevels = document.attr(:sectnumlevels, DEFAULT_SECTNUMLEVELS).to_i

    if sec.numbered && !sec.caption && sec.level <= sectnumlevels
      [sec.sectnum, sec.captioned_title].join(' ')
    else
      sec.captioned_title
    end
  end

  #--------------------------------------------------------
  # block_listing
  #

  def source_lang
    attr :language, nil, false
  end

  def map_to_confluence_supported_lang(lang)
    if lang.nil?
      result = nil
    else
      mapper = document.attr 't2c-language-mapper'
      result = mapper.map_to_confluence_language(lang)
    end
    result
  end

  def macro_param_if_present_in_attr(name)
    if attributes.key? name
      value = attributes[name]
      %(<ac:parameter ac:name="#{name}">#{value}</ac:parameter>)
    else
      ""
    end
  end

  def unescaped(content)
    decoder = document.attr 't2c-decoder'
    decoder.convert(content)
  end

  def page_attachment(target)
    attachment_collector = document.attr 't2c-attachments-collector'
    attachment_collector.collect(target)
  end

  def resolve_xref(target)
    ref_provider = document.attr 't2c-ref-provider'
    ref_provider.resolve_xref target
  end

  def resolve_link(target)
    ref_provider = document.attr 't2c-ref-provider'
    ref_provider.resolve_link target
  end

  #--------------------------------------------------------
  # inline_anchor
  # @param empty_fallback [String] optional fallback to use when reftext is empty
  # @return [String, nil] text of the xref anchor, or +nil+ if not found.
  def xref_text empty_fallback
    if attributes[:refid] == text
      ref = document.catalog[:refs][attributes['refid'] || target]
      str = (ref ? ref.xreftext(attr 'xrefstyle', nil, true) : empty_fallback || text)
    else
      str = text
    end
    str.tr_s("\n", ' ') if str
  end

  # Public: Efficiently checks whether the specified String resembles a URI
  #
  # Uses the Asciidoctor::UriSniffRx regex to check whether the String begins
  # with a URI prefix (e.g., http://). No validation of the URI is performed.
  #
  # str - the String to check
  #
  # @return true if the String is a URI, false if it is not
  def uriish? str
    (str.include? ':') && str =~ UriSniffRx
  end

  # removes leading hash from anchor targets
  def anchor_name str
    if str.include? "#"
      str[(str.index('#')+1)..str.length]
    else
      str
    end
  end

  def line_break
    "\n"
  end

  # generates confluence anchor macro
  # @param name [String] id of anchor
  def anchor(name)
    %(<ac:structured-macro ac:name="anchor"><ac:parameter ac:name="">#{name}</ac:parameter></ac:structured-macro>)
  end

  # generates a link to confluence macro
  # @param name [String] id of anchor
  # @param body_text [String] text of generated link
  def anchor_link(name, body_text)
    %(<ac:link ac:anchor="#{name}"><ac:plain-text-link-body><![CDATA[#{body_text}]]></ac:plain-text-link-body></ac:link>)
  end

  ##
  # @param index [Integer] the footnote's index.
  # @return [String] footnote id to be used in a link.
  def footnote_id(index = (attr :index))
    %(_footnotedef_#{index})
  end

  ##
  # @param index (see #footnote_id)
  # @return [String] footnote anchor id
  def footnoteref_id(index = (attr :index))
    %(_footnoteref_#{index})
  end

  ## Surrounds passed block with strings
  def surround(front, back = front)
    [front, yield.chomp, back].join
  end

  ## customization for inline quoted conversion to render <del> tag instead of line-through class for span
  def confluence_inline_quoted node
    open, close, tag = QUOTE_TAGS[node.type]
    if node.id
      class_attr = node.role ? %( class="#{node.role}") : ''
      if tag
        %(#{open.chop} id="#{node.id}"#{class_attr}>#{node.text}#{close})
      else
        %(<span id="#{node.id}"#{class_attr}>#{open}#{node.text}#{close}</span>)
      end
    elsif node.role
      if tag
        %(#{open.chop} class="#{node.role}">#{node.text}#{close})
      elsif node.role == 'line-through'
        %(<del>#{node.text}</del>)
      elsif node.role == 'underline'
        %(<u>#{node.text}</u>)
      else
        %(<span class="#{node.role}">#{open}#{node.text}#{close}</span>)
      end
    else
      %(#{open}#{node.text}#{close})
    end
  end

  # @param text [String]
  def link_content text
    if text.include?('<') || text.include?('&#')
      %(<ac:link-body>#{text}</ac:link-body>)
    else
      %(<ac:plain-text-link-body><![CDATA[#{text}]]></ac:plain-text-link-body>)
    end
  end

  def editor_version
    if document.attr? 'property_editor'
      document.attr 'property_editor'
    else
      document.attr 't2c-editor-version', 'v1'
    end
  end

  def adjusted_paragraph_content
    content.gsub(Asciidoctor::LF, ' ')
  end

  def escape_quotes val
    val.gsub(/"/, '&quot;'.freeze)
  end

  def escape_xml_attr val
    decoder = document.attr 't2c-decoder'
    decoder.escapeXml(val)
  end

  def confluence_text_alignment
    case role
    when 'text-left'
      'text-align: left;'
    when 'text-right'
      'text-align: right;'
    when 'text-center'
      'text-align: center;'
    when 'text-justify'
      'text-align: justify;'
    else
      nil
    end
  end

  def table_cell_styling cell
    attributes = {}
    styles = []
    if cell.attr('halign') != 'left'
      styles.push("text-align: #{cell.attr 'halign'};")
    end
    if editor_version() == 'v1' and cell.attr('valign') != 'top'
      styles.push("vertical-align: #{cell.attr 'valign'};")
    end
    unless styles.empty?
      attributes["style"] = styles.join(' ')
    end
    if document.attr? 'cellbgcolor'
      cellColor = document.attr 'cellbgcolor'
      attributes['data-highlight-colour'] = "#{cellColor}"
      if editor_version() == 'v1'
        attributes['class'] = "highlight-#{cellColor}"
      end
    end
    return attributes
  end

  # method for editor v2 pages, returns page width settings
  def page_layout
    document.attr 'property_content-appearance-published', 'default'
  end

  TABLE_WIDTH_NARROW = 760
  TABLE_WIDTH_WIDE = 1800

  # Returns integer target width in pixels for the table when using editor v2.
  # - returns nil for editor v1 (v1 doesn't use pixel table width for colgroup)
  # - uses page layout (full-width -> wide) to pick base width
  # - when a custom table width is present (attr :width) interprets attr(:tablepcwidth)
  #   as percentage (e.g. 50) and calculates pixels as base * percent/100
  def table_target_width
    return nil unless editor_version() == 'v2'

    if attr? :width
      pc = attr(:tablepcwidth).to_f
      return (TABLE_WIDTH_WIDE * (pc / 100.0)).to_i
    end

    layout = page_layout() == 'full-width' ? 'wide' : 'narrow'
    (layout == 'wide' ? TABLE_WIDTH_WIDE : TABLE_WIDTH_NARROW)
  end

  def table_attributes
    def table_layout layout, narrow_value
      layout == 'wide' ? (role == 'center' ? 'center' : 'align-start') : narrow_value
    end
    if editor_version() == 'v1'
        return {:class => 'relative-table', :style => (attr?(:width) ? "width: #{attr :tablepcwidth}%" : nil) }
    elsif editor_version() == 'v2'
        layout = page_layout() == 'full-width' ? 'wide' : 'narrow'
        pxl_width = table_target_width
        if attr? :width
            return {'data-table-width' => pxl_width, 'data-layout' => table_layout(layout, 'center')}
        else
            return {'data-table-width' => pxl_width, 'data-layout' => table_layout(layout, 'default')}
        end
    end

  end


end
