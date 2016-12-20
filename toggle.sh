TOOLKIT_ROOT="/ebio/abt1_share/toolkit_dev/lzimmermann/toolkit_sky"
BASENAME="/ebio/abt1_share/toolkit_dev/lzimmermann/databases/genomes_new/current"


if [ -e  ${TOOLKIT_ROOT}/public/javascripts/dnatree.js_old   ] ; then

    # Revert back to old state
    rm "${TOOLKIT_ROOT}/public/javascripts/dnatree.js"
    mv "${TOOLKIT_ROOT}/public/javascripts/dnatree.js_old" "${TOOLKIT_ROOT}/public/javascripts/dnatree.js"

    rm "${TOOLKIT_ROOT}/public/javascripts/proteintree.js" 
    mv "${TOOLKIT_ROOT}/public/javascripts/proteintree.js_old" "${TOOLKIT_ROOT}/public/javascripts/proteintree.js"

    
    rm "${TOOLKIT_ROOT}/app/views/genomes/_dnatree.rhtml" 
    mv "${TOOLKIT_ROOT}/app/views/genomes/_dnatree.rhtml_old" "${TOOLKIT_ROOT}/app/views/genomes/_dnatree.rhtml"

    rm "${TOOLKIT_ROOT}/app/views/genomes/_proteintree.rhtml" 
    mv "${TOOLKIT_ROOT}/app/views/genomes/_proteintree.rhtml_old" "${TOOLKIT_ROOT}/app/views/genomes/_proteintree.rhtml"
else

    
    mv "${TOOLKIT_ROOT}/public/javascripts/dnatree.js" "${TOOLKIT_ROOT}/public/javascripts/dnatree.js_old"
    ln -s  ${BASENAME}/web/dnatree.js  "${TOOLKIT_ROOT}/public/javascripts/dnatree.js"

    mv "${TOOLKIT_ROOT}/public/javascripts/proteintree.js" "${TOOLKIT_ROOT}/public/javascripts/proteintree.js_old"
    ln -s  ${BASENAME}/web/proteintree.js  "${TOOLKIT_ROOT}/public/javascripts/proteintree.js"

    mv "${TOOLKIT_ROOT}/app/views/genomes/_dnatree.rhtml" "${TOOLKIT_ROOT}/app/views/genomes/_dnatree.rhtml_old"
    ln -s  ${BASENAME}/web/_dnatree.rhtml  "${TOOLKIT_ROOT}/app/views/genomes/_dnatree.rhtml"


    mv "${TOOLKIT_ROOT}/app/views/genomes/_proteintree.rhtml" "${TOOLKIT_ROOT}/app/views/genomes/_proteintree.rhtml_old"
    ln -s  ${BASENAME}/web/_proteintree.rhtml  "${TOOLKIT_ROOT}/app/views/genomes/_proteintree.rhtml"


fi



#cp /ebio/abt1_share/toolkit_dev/lzimmermann/databases/genomes_new/current/web/dnatree.js /cluster/www/toolkit/public/javascripts/dnatree.js;
#cp /ebio/abt1_share/toolkit_dev/lzimmermann/databases/genomes_new/current/web/proteintree.js /cluster/www/toolkit/public/javascripts/proteintree.js;
#cp /ebio/abt1_share/toolkit_dev/lzimmermann/databases/genomes_new/current/web/_dnatree.rhtml /cluster/www/toolkit/app/views/genomes/_dnatree.rhtml;
#cp /ebio/abt1_share/toolkit_dev/lzimmermann/databases/genomes_new/current/web/_proteintree.rhtml /cluster/www/toolkit/app/views/genomes/_proteintree.rhtml;



