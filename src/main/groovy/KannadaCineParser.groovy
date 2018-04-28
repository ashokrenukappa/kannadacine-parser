import groovy.json.JsonOutput
import groovy.text.SimpleTemplateEngine
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.Node
import groovy.util.slurpersupport.NodeChild
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException

@Slf4j
class KannadaCineParser {

    final static String MOVIES_INPUT_JSON = 'src/main/angularjs/kannada_movies_cine_input.json'
    final static String MOVIES_INDEX_HTML_TEMPLATE = 'src/main/angularjs/kannada_movies_cine_index.html.template'
    final static String MOVIES_INDEX_OUTPUT_HTML = 'src/main/angularjs/kannada_movies_cine_index.html'

    static class MovieInfo{
        String title
        int recency
        List videoUrls

        MovieInfo(String title, List videoUrls, int recency) {
            this.title = title
            this.videoUrls = videoUrls
            this.recency = recency
        }
    }
    
    MovieInfo extractMovieVideoUrls(String url, int recency){
        def http = new HTTPBuilder(url)
        def html = http.get([:])

        //log.debug("Loading page: {}", url)

        List<Node> childNodes = ((NodeChild)html).childNodes() as List
        List<Node> video1VideoNodes = filterNodes(childNodes, "id", "fp-video-0")

        List<Node> titleNodes = filterNodesByTag(((NodeChild)html).childNodes() as List, "title")

        String movieTitle = "no-title"
        if(titleNodes){
            movieTitle = titleNodes.get(0).text()
        }

        List<String> video1Urls = extractAttributeValues(video1VideoNodes,"src")

        new MovieInfo(movieTitle, video1Urls, recency)
    }


    void writeHtml(def moviesList){
        def json = JsonOutput.toJson(moviesList)

        new File(MOVIES_INPUT_JSON).text = JsonOutput.prettyPrint(json)

        def templateFile = new File(MOVIES_INDEX_HTML_TEMPLATE)

        def binding = ["moviesData": JsonOutput.prettyPrint(json) ]
        def engine = new SimpleTemplateEngine()
        def template = engine.createTemplate(templateFile.text).make(binding)

        def htmlFile = new File(MOVIES_INDEX_OUTPUT_HTML)
        htmlFile.text = template.toString()
        println ("Open ${htmlFile.absolutePath}")
    }

    List<String> extractMoviePageUrls(String url){
        def http = new HTTPBuilder(url)
        def html = http.get([:])

        List<Node> childNodes = ((NodeChild)html).childNodes() as List
        List<Node> movieDivNodes = filterNodes(childNodes, "class", "featured-image")
        List<String> movieUrls = extractAttributeValues(movieDivNodes, "href")

        movieUrls
    }


    List<Node> filterNodesByTag(List<Node> nodes, String tagType){

        List<Node> filteredNodes = []
        for(Node node: nodes){
            if(tagType.equalsIgnoreCase(node.name())){
                filteredNodes.add( node )
            }
            if(node.childNodes()){
                filteredNodes.addAll( filterNodesByTag(node.childNodes() as List,tagType) )
            }
        }

        filteredNodes
    }

    List<Node> filterNodes(List<Node> nodes, String attributeKey, String attributeValue){

        List<Node> filteredNodes = []
        for(Node nodeChild: nodes){
            if(attributeValue.equalsIgnoreCase(nodeChild.attributes().get(attributeKey) as String)){
                filteredNodes.add( nodeChild )
            }
            if(nodeChild.childNodes()){
                filteredNodes.addAll( filterNodes(nodeChild.childNodes() as List, attributeKey, attributeValue) )
            }
        }

        filteredNodes
    }

    List<Node> filterNodes(List<Node> nodes, String attributeKey){

        List<Node> filteredNodes = []
        for(Node nodeChild: nodes){
            if(null != nodeChild.attributes().get(attributeKey)){
                filteredNodes.add( nodeChild )
            }
            if(nodeChild.childNodes()){
                filteredNodes.addAll( filterNodes(nodeChild.childNodes() as List, attributeKey) )
            }
        }

        filteredNodes
    }

    List<String> extractAttributeValues(List<Node> nodes, String attributeKey){

        List<String> attributeValues = []
        List<Node> filteredNodes = filterNodes(nodes, attributeKey)
        for(Node nodeChild: filteredNodes){
            attributeValues.add( nodeChild.attributes().get(attributeKey) as String )
        }

        attributeValues
    }


    static void main(String []a){

        KannadaCineParser kcParser = new KannadaCineParser()
        List<MovieInfo> movieInfoList = []

        int i = 1
        int itemCount = 1
        try {
            while (true){
                String pageUrl = "http://www.kannadacine.com/page/${i}/"

                long start = System.currentTimeMillis()
                List<String> moviePageUrls = kcParser.extractMoviePageUrls("http://www.kannadacine.com/page/${i}/")

                for(String url:moviePageUrls){
                    movieInfoList.add(kcParser.extractMovieVideoUrls(url, itemCount++ ))
                }
                i++
                log.info("Completed page: {} , took: {} secs.", pageUrl, (System.currentTimeMillis() - start)/ 1000)
            }
        } catch (HttpResponseException exception) {
            println("Terminating at page ${i} error : ${exception.getMessage()}")
        }
        log.info("Found total: {} movie / videos.", itemCount)
        kcParser.writeHtml(movieInfoList)

    }


    /*

    Video1
    <div class="fluid_video_wrapper fluid_player_layout_funky" id="fluid_video_wrapper_fp-video-0" style="height: 385px; width: 770px;"><video id="fp-video-0" style="width: 100%; height: 100%;">
    <source title="720" src="http://votrefiles.com/f/df/danakayonu.mp4" type="video/mp4">
</video><div class="vast_video_loading" id="vast_video_loading_fp-video-0" style="display: none;"></div><div id="fp-video-0_fluid_controls_container" class="fluid_controls_container"><div class="fluid_controls_left">   <div id="fp-video-0_fluid_control_playpause" class="fluid_button fluid_button_play"></div></div><div id="fp-video-0_fluid_controls_progress_container" class="fluid_controls_progress_container fluid_slider">   <div class="fluid_controls_progress">      <div id="fp-video-0_vast_control_currentprogress" class="fluid_controls_currentprogress">          <div id="fp-video-0_vast_control_currentpos" class="fluid_controls_currentpos"></div>      </div>   </div></div><div class="fluid_controls_right">   <div id="fp-video-0_fluid_control_fullscreen" class="fluid_button fluid_button_fullscreen"></div>   <div id="fp-video-0_fluid_control_video_source" class="fluid_button_video_source"></div>   <div id="fp-video-0_fluid_control_volume_container" class="fluid_control_volume_container fluid_slider">       <div id="fp-video-0_fluid_control_volume" class="fluid_control_volume">           <div id="fp-video-0_fluid_control_currentvolume" class="fluid_control_currentvolume" style="width: 75px;">               <div id="fp-video-0_fluid_control_volume_currentpos" class="fluid_control_volume_currentpos" style="left: 69px;"></div>           </div>       </div>   </div>   <div id="fp-video-0_fluid_control_mute" class="fluid_button fluid_button_volume"></div>   <div id="fp-video-0_fluid_control_duration" class="fluid_fluid_control_duration">00:00 / 00:00</div></div></div><div id="fp-video-0_fluid_context_menu" class="fluid_context_menu" style="display: none; position: absolute;"><ul>    <li id="fp-video-0context_option_play">Play</li>    <li id="fp-video-0context_option_mute">Mute</li>    <li id="fp-video-0context_option_fullscreen">Fullscreen</li>    <li id="fp-video-0context_option_homepage">       <a id="fp-video-0context_option_homepage_link" href="https://www.fluidplayer.com/" target="_blank" style="color: inherit; text-decoration: inherit;">           Fluidplayer 1.2.2       </a>     </li></ul></div></div>

    Video2

    <div id="squelch-taas-accordion-shortcode-content-1" class="squelch-taas-accordion-shortcode-content squelch-taas-accordion-shortcode-content-1 ui-accordion-content ui-helper-reset ui-widget-content ui-corner-bottom ui-accordion-content-active" aria-labelledby="squelch-taas-header-1" role="tabpanel" aria-hidden="false" style="display: block;"><iframe frameborder="0" width="480" height="270" src="https://oload.info/embed/pJiIL747VGw/DanaKayonu" allowfullscreen=""></iframe></div>

     */

}
